package com.wams.quotation;import org.springframework.data.jpa.repository.JpaRepository;import java.util.List;
public interface QuotationRepository extends JpaRepository<QuotationRequest,Long>{List<QuotationRequest>findAllByProjectIdOrderByCreatedAtDesc(Long id);List<QuotationRequest>findAllByVendorIdOrderByCreatedAtDesc(Long id);}
