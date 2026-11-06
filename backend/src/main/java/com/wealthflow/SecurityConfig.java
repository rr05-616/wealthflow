package com.wealthflow;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;
@Configuration
public class SecurityConfig {
 private SecretKey key(String secret){return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");}
 @Bean JwtDecoder jwtDecoder(@Value("${wealthflow.jwt.secret}")String secret){return NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();}
 @Bean JwtEncoder jwtEncoder(@Value("${wealthflow.jwt.secret}")String secret){return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));}
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
  var converter=new JwtAuthenticationConverter();
  converter.setJwtGrantedAuthoritiesConverter(jwt->{String role=jwt.getClaimAsString("role");String authority="billingOfficer".equals(role)?"ROLE_BILLING_OFFICER":"labUser".equals(role)?"ROLE_LAB_USER":"ROLE_INVALID";return List.of(new SimpleGrantedAuthority(authority));});
  return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.requestMatchers("/api/chargebacks/calculate").hasAnyRole("LAB_USER","BILLING_OFFICER").requestMatchers("/api/chargebacks/*/finalize").hasRole("BILLING_OFFICER").requestMatchers("/api/workflows/**").authenticated().anyRequest().permitAll()).oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
 }
}
