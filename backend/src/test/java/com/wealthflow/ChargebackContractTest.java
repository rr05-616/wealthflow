package com.wealthflow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest @AutoConfigureMockMvc @TestPropertySource(properties={"spring.kafka.listener.auto-startup=false","wealthflow.jwt.secret=contract-test-secret-at-least-32-bytes"})
class ChargebackContractTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JwtEncoder jwtEncoder;
 private String jwt(){var now=Instant.now();var claims=JwtClaimsSet.builder().subject("billing-test").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("role","billingOfficer").build();return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();}
 @Test void responseAndEventContractContainRequiredFields()throws Exception{
  var result=mvc.perform(post("/api/chargebacks/calculate").header("Authorization","Bearer "+jwt()).contentType("application/json").content("{\"totalAmount\":100.00,\"allocations\":[{\"id\":\"A\",\"share\":33333},{\"id\":\"B\",\"share\":33333},{\"id\":\"C\",\"share\":33334}]}" )).andExpect(status().isOk()).andReturn();
  var json=mapper.readTree(result.getResponse().getContentAsString());var responseFields=new java.util.HashSet<String>();json.fieldNames().forEachRemaining(responseFields::add);assertThat(responseFields).contains("chargebackId","originalTotal","exactShares","roundingResidual","finalAllocations","reconciledTotal","reconciled");assertThat(json.get("reconciled").asBoolean()).isTrue();assertThat(json.get("finalAllocations").get(2).get("finalAmount").decimalValue()).isEqualByComparingTo("33.34");
  var event=new ChargebackEvent("event-1","ChargebackFinalized","cb-1","billing-test",json.get("originalTotal").decimalValue(),json.get("reconciledTotal").decimalValue(),"DEMO",Instant.now());var eventJson=mapper.valueToTree(event);var eventFields=new java.util.HashSet<String>();eventJson.fieldNames().forEachRemaining(eventFields::add);assertThat(eventFields).contains("eventId","eventType","chargebackId","ownerId","totalBilledAmount","finalAllocatedAmount","rateVersion","finalizedAt");
 }
}
