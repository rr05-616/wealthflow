package com.wealthflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @EmbeddedKafka(partitions=1,topics={"soa.s08.t24.events"},bootstrapServersProperty="spring.kafka.bootstrap-servers") @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ChargebackWorkflowSpringBootTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired WorkflowRepository workflows;@Autowired JwtEncoder jwtEncoder;
 private String jwt(String role){return jwt(role,"demo-billing-user");}private String jwt(String role,String subject){var now=Instant.now();var claims=JwtClaimsSet.builder().subject(subject).issuedAt(now).expiresAt(now.plusSeconds(300)).claim("role",role).build();return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();}
 @Test void authenticatedApiFinalizesPublishesAndPersistsConsumedEvent()throws Exception{
  String body="{\"totalAmount\":100.00,\"allocations\":[{\"id\":\"A\",\"share\":33333},{\"id\":\"B\",\"share\":33333},{\"id\":\"C\",\"share\":33334}]}";
  mvc.perform(post("/api/chargebacks/calculate").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  MvcResult calculated=mvc.perform(post("/api/chargebacks/calculate").header("Authorization","Bearer "+jwt("labUser")).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.reconciled").value(true)).andExpect(jsonPath("$.finalAllocations[2].finalAmount").value(33.34)).andReturn();
  JsonNode response=mapper.readTree(calculated.getResponse().getContentAsString());String id=response.get("chargebackId").asText();
  mvc.perform(post("/api/chargebacks/"+id+"/finalize").header("Authorization","Bearer "+jwt("labUser")).contentType("application/json").content(response.toString())).andExpect(status().isForbidden());
  mvc.perform(post("/api/chargebacks/"+id+"/finalize").header("Authorization","Bearer "+jwt("billingOfficer")).contentType("application/json").content(response.toString())).andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("FINALIZED"));
  long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(System.nanoTime()<until&&!workflows.existsById(id))Thread.sleep(100);
  var state=workflows.findById(id);assertThat(state).isPresent();assertThat(state.get().getStatus()).isEqualTo("FINALIZED");assertThat(state.get().getOwnerId()).isEqualTo("demo-billing-user");assertThat(state.get().getTotalBilledAmount()).isEqualByComparingTo("100.00");assertThat(state.get().getFinalAllocatedAmount()).isEqualByComparingTo("100.00");System.out.println("CONSUMED_EVENT_STATE="+mapper.writeValueAsString(java.util.Map.of("eventId",state.get().getEventId(),"eventType","ChargebackFinalized","chargebackId",id,"ownerId",state.get().getOwnerId(),"totalBilledAmount",state.get().getTotalBilledAmount(),"finalAllocatedAmount",state.get().getFinalAllocatedAmount(),"rateVersion","DEMO","finalizedAt",state.get().getFinalizedAt(),"status",state.get().getStatus())));
  mvc.perform(get("/api/workflows/"+id).header("Authorization","Bearer "+jwt("labUser"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZED")).andExpect(jsonPath("$.totalBilledAmount").value(100.0)).andExpect(jsonPath("$.finalAllocatedAmount").value(100.0));
  mvc.perform(get("/api/workflows/"+id).header("Authorization","Bearer "+jwt("labUser","another-user"))).andExpect(status().isForbidden());
 }
}
