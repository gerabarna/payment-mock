package hu.gerab.payment.service;

import java.math.BigDecimal;

public interface PaymentService {

  void processTransaction(String requestId, long userId, BigDecimal amount, String currency);
}
