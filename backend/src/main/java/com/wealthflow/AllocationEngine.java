package com.wealthflow;
import java.math.*;import java.util.*;
public final class AllocationEngine {
 private AllocationEngine(){}
 public record Share(String id,BigDecimal weight){}
 public record Line(String id,BigDecimal exactShare,BigDecimal roundedAmount,BigDecimal finalAmount,int residualMinorUnits){}
 public record Result(BigDecimal billedTotal,List<Line> lines,BigDecimal residual,BigDecimal reconciledTotal,boolean reconciled){}
 public static Result allocate(BigDecimal billedTotal,List<Share> shares){
  if(billedTotal==null||billedTotal.signum()<=0||billedTotal.scale()>2)throw new IllegalArgumentException("Billed total must be positive INR with paise precision");
  if(shares==null||shares.size()<2)throw new IllegalArgumentException("At least two shares are required");
  var ids=new HashSet<String>();for(var s:shares)if(s.id()==null||s.id().isBlank()||!ids.add(s.id())||s.weight()==null||s.weight().signum()<=0)throw new IllegalArgumentException("Recipient IDs must be unique and weights positive");
  BigDecimal sum=shares.stream().map(Share::weight).reduce(BigDecimal.ZERO,BigDecimal::add);var unit=new BigDecimal("0.01");
  record Part(Share share,BigDecimal exact,BigDecimal floor,BigDecimal remainder){}
  var parts=shares.stream().map(s->{var exact=billedTotal.multiply(s.weight()).divide(sum,18,RoundingMode.HALF_EVEN);var floor=exact.setScale(2,RoundingMode.DOWN);return new Part(s,exact,floor,exact.subtract(floor));}).toList();
  BigDecimal residual=billedTotal.subtract(parts.stream().map(Part::floor).reduce(BigDecimal.ZERO,BigDecimal::add));int units=residual.divide(unit,0,RoundingMode.UNNECESSARY).intValueExact();
  var order=parts.stream().sorted(Comparator.comparing(Part::remainder).reversed().thenComparing(p->p.share().id())).toList();var winners=new HashSet<String>();for(int i=0;i<units;i++)winners.add(order.get(i%order.size()).share().id());
  var lines=parts.stream().map(p->{boolean win=winners.contains(p.share().id());return new Line(p.share().id(),p.exact(),p.floor(),p.floor().add(win?unit:BigDecimal.ZERO),win?1:0);}).toList();var finalTotal=lines.stream().map(Line::finalAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
  return new Result(billedTotal,lines,residual,finalTotal,finalTotal.compareTo(billedTotal)==0);
 }
}
