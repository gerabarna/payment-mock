package hu.gerab.payment.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.concurrent.Future;

public interface PaymentService {

  Future<Boolean> processTransaction(
      String requestId, long userId, BigDecimal amount, String currency);
}
