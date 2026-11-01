package com.wams.quotation.dto;import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record SubmitQuotationRequest(@NotNull@DecimalMin(value="0",inclusive=false)@Digits(integer=12,fraction=2)BigDecimal amount,@NotBlank@Size(max=2000)String description){}
