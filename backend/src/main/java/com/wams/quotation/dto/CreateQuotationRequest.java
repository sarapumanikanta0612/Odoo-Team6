package com.wams.quotation.dto;import jakarta.validation.constraints.*;import java.time.LocalDate;
public record CreateQuotationRequest(@NotNull@Positive Long vendorId,Long functionId,@NotBlank@Size(max=150)String service,@NotNull@FutureOrPresent LocalDate deadline){}
