package com.wams.enquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wams.enquiry.dto.EnquiryResponse;
import com.wams.enquiry.dto.PublicEnquiryRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test for the enquiry rules. Uses a mocked repository, so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
class EnquiryServiceTest {

	@Mock
	private EnquiryRepository enquiryRepository;

	@InjectMocks
	private EnquiryService enquiryService;

	@Test
	void createPublicEnquiry_recordsNewWebsiteEnquiryWithNormalisedInput() {
		LocalDate weddingStart = LocalDate.now().plusMonths(6);
		LocalDate consultation = LocalDate.now().plusWeeks(2);
		PublicEnquiryRequest request = new PublicEnquiryRequest(
				"  Priya Sharma ",
				" Priya.Sharma@Example.COM ",
				" +91 98765 43210 ",
				weddingStart,
				weddingStart.plusDays(2),
				" Udaipur ",
				350,
				new BigDecimal("2500000.00"),
				new BigDecimal("4000000.00"),
				"Decor, catering, photography",
				"   ",
				"Mehendi, Sangeet, Wedding, Reception",
				null,
				" Please call after 6 pm ",
				consultation);
		when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(invocation -> {
			Enquiry enquiry = invocation.getArgument(0);
			enquiry.setId(42L);
			return enquiry;
		});

		EnquiryResponse response = enquiryService.createPublicEnquiry(request);

		ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
		verify(enquiryRepository).save(captor.capture());
		Enquiry saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo(EnquiryStatus.NEW);
		assertThat(saved.getSource()).isEqualTo(EnquirySource.WEBSITE);
		assertThat(saved.getContactName()).isEqualTo("Priya Sharma");
		assertThat(saved.getEmail()).isEqualTo("priya.sharma@example.com");
		assertThat(saved.getPhone()).isEqualTo("+91 98765 43210");
		assertThat(saved.getWeddingStartDate()).isEqualTo(weddingStart);
		assertThat(saved.getWeddingEndDate()).isEqualTo(weddingStart.plusDays(2));
		assertThat(saved.getDestination()).isEqualTo("Udaipur");
		assertThat(saved.getEstimatedGuestCount()).isEqualTo(350);
		assertThat(saved.getBudgetMin()).isEqualByComparingTo("2500000.00");
		assertThat(saved.getBudgetMax()).isEqualByComparingTo("4000000.00");
		assertThat(saved.getServicesOfInterest()).isEqualTo("Decor, catering, photography");
		assertThat(saved.getThemeStyle()).as("blank optional text is stored as null").isNull();
		assertThat(saved.getFunctionsRequired()).isEqualTo("Mehendi, Sangeet, Wedding, Reception");
		assertThat(saved.getSpecialRequests()).isNull();
		assertThat(saved.getMessage()).isEqualTo("Please call after 6 pm");
		assertThat(saved.getPreferredConsultationDate()).isEqualTo(consultation);

		assertThat(response.id()).isEqualTo(42L);
		assertThat(response.status()).isEqualTo(EnquiryStatus.NEW);
		assertThat(response.source()).isEqualTo(EnquirySource.WEBSITE);
		assertThat(response.email()).isEqualTo("priya.sharma@example.com");
	}

}
