package com.wealthflow;
import java.time.Instant;import java.math.BigDecimal;
public record ChargebackEvent(String eventId,String eventType,String chargebackId,String ownerId,BigDecimal totalBilledAmount,BigDecimal finalAllocatedAmount,String rateVersion,Instant finalizedAt){}
