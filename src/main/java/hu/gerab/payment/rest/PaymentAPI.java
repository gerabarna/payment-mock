package hu.gerab.payment.rest;

import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.QueryParam;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("payment")
@Tag(name = "Payment")
// @SecurityRequirement() TBD
public interface PaymentAPI {

  @Operation(
      summary = "Handles a simple transaction for the user",
      responses = {
        @ApiResponse(content = @Content(schema = @Schema(implementation = String.class)))
      })
  @PostMapping("transaction")
  String handleTransaction(
      @Parameter(description = "The id of the user", in = QUERY) @QueryParam("userId") Long userId,
      @Parameter(description = "The amount with which to change the user balance", in = QUERY)
          @QueryParam("amount")
          BigDecimal amount,
      @Parameter(
              description =
                  "The currency of the transaction. Only USD transactions are accepted for now",
              in = QUERY)
          @QueryParam("currency")
          String currency);
}
