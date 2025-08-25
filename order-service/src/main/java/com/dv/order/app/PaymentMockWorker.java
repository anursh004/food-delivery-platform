package com.dv.order.app;
import com.dv.order.domain.OrderEntity;
import com.dv.order.domain.OrderStatus;
import com.dv.order.repo.OrderQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.Random;
@Component
public class PaymentMockWorker {
  private static final Logger log = LoggerFactory.getLogger(PaymentMockWorker.class);
  private final OrderQuery query;
  private final Random rnd = new Random();
  private final int batch;
  private final int minDelayMs;
  private final int maxDelayMs;
  public PaymentMockWorker(OrderQuery query,
                           @Value("${app.payment.batchSize:20}") int batch,
                           @Value("${app.payment.minDelayMs:50}") int minDelayMs,
                           @Value("${app.payment.maxDelayMs:150}") int maxDelayMs){
    this.query=query; this.batch=batch; this.minDelayMs=minDelayMs; this.maxDelayMs=maxDelayMs;
  }
  @Scheduled(fixedDelayString = "PT2S")
  @Transactional
  public void sweep(){
    List<OrderEntity> created = query.findCreated();
    int n = Math.min(batch, created.size());
    for (int i=0;i<n;i++){
      OrderEntity o = created.get(i);
      // simulate variable gateway time
      try { Thread.sleep(rnd.nextInt(Math.max(1, maxDelayMs-minDelayMs)) + minDelayMs); } catch (InterruptedException ignored) {}
      o.setStatus(OrderStatus.PAID);
      log.info("Mock-paid order {}", o.getId());
    }
  }
}
