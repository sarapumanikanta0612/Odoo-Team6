package com.wams.enquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.wams.repository.UserRepository;
import com.wams.security.JwtService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Web-layer test for POST /api/public/enquiries: JSON binding, validation, status codes and
 * the NEW/WEBSITE rule, using the real EnquiryService and a mocked repository.
 * <p>
 * Spring Security filters are skipped at MockMvc level ({@code addFilters = false}) so
 * these tests focus on controller/service behavior. The anonymous-access guarantee for
 * {@code POST /api/public/enquiries} is covered end-to-end by
 * {@code com.wams.config.SecurityConfigIntegrationTest} against the real filter chain.
 */
@WebMvcTest(PublicEnquiryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(EnquiryService.class)
class PublicEnquiryControllerTest {

	private static final String URL = "/api/public/enquiries";
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final Instant SAVED_AT = Instant.parse("2026-10-31T10:15:30Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EnquiryRepository enquiryRepository;

	/**
	 * {@code @WebMvcTest} loads {@code Filter} beans but not {@code @Service} beans, so
	 * {@link com.wams.security.JwtAuthenticationFilter} cannot find its JwtService and
	 * UserRepository dependencies. We provide mocks purely to satisfy construction —
	 * the filter is never invoked here because {@code addFilters = false}.
	 */
	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	void validRequest_returns201WithNewWebsiteEnquiry() throws Exception {
		stubSaveAssigningId(1L);

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(validBody())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.source").value("WEBSITE"))
				.andExpect(jsonPath("$.contactName").value("Priya Sharma"))
				.andExpect(jsonPath("$.email").value("priya.sharma@example.com"))
				.andExpect(jsonPath("$.weddingStartDate").value(weddingStart().toString()))
				.andExpect(jsonPath("$.estimatedGuestCount").value(350))
				.andExpect(jsonPath("$.createdAt").value(SAVED_AT.toString()));

		verify(enquiryRepository).save(any(Enquiry.class));
	}

	@Test
	void statusSourceAndIdInBody_areIgnored() throws Exception {
		stubSaveAssigningId(1L);
		Map<String, Object> body = validBody();
		body.put("status", "CONVERTED");
		body.put("source", "REFERRAL");
		body.put("id", 999);

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(body)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.source").value("WEBSITE"));

		ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
		verify(enquiryRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(EnquiryStatus.NEW);
		assertThat(captor.getValue().getSource()).isEqualTo(EnquirySource.WEBSITE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void invalidRequest_returns400AndSavesNothing(String description, Consumer<Map<String, Object>> change)
			throws Exception {
		Map<String, Object> body = validBody();
		change.accept(body);

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(body)))
				.andExpect(status().isBadRequest());

		verify(enquiryRepository, never()).save(any(Enquiry.class));
	}

	@Test
	void malformedJson_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"contactName\": "))
				.andExpect(status().isBadRequest());

		verify(enquiryRepository, never()).save(any(Enquiry.class));
	}

	static Stream<Arguments> invalidRequests() {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		return Stream.of(
				invalid("contactName missing", body -> body.remove("contactName")),
				invalid("contactName blank", body -> body.put("contactName", "   ")),
				invalid("contactName over 120 characters", body -> body.put("contactName", "a".repeat(121))),
				invalid("email missing", body -> body.remove("email")),
				invalid("email invalid", body -> body.put("email", "not-an-email")),
				invalid("phone missing", body -> body.remove("phone")),
				invalid("phone with letters", body -> body.put("phone", "98765abcde")),
				invalid("phone under 7 digits", body -> body.put("phone", "12345")),
				invalid("wedding start date in the past", body -> body.put("weddingStartDate", yesterday.toString())),
				invalid("wedding end date before start date",
						body -> body.put("weddingEndDate", weddingStart().minusDays(1).toString())),
				invalid("wedding end date without start date", body -> body.remove("weddingStartDate")),
				invalid("guest count zero", body -> body.put("estimatedGuestCount", 0)),
				invalid("negative budget", body -> body.put("budgetMin", -1)),
				invalid("budgetMax below budgetMin", body -> {
					body.put("budgetMin", 4000000);
					body.put("budgetMax", 2500000);
				}),
				invalid("consultation date in the past",
						body -> body.put("preferredConsultationDate", yesterday.toString())));
	}

	private static Arguments invalid(String description, Consumer<Map<String, Object>> change) {
		return Arguments.of(description, change);
	}

	private static LocalDate weddingStart() {
		return LocalDate.now().plusMonths(6);
	}

	private static Map<String, Object> validBody() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contactName", "Priya Sharma");
		body.put("email", "Priya.Sharma@example.com");
		body.put("phone", "+91 98765 43210");
		body.put("weddingStartDate", weddingStart().toString());
		body.put("weddingEndDate", weddingStart().plusDays(2).toString());
		body.put("destination", "Udaipur");
		body.put("estimatedGuestCount", 350);
		body.put("budgetMin", new BigDecimal("2500000.00"));
		body.put("budgetMax", new BigDecimal("4000000.00"));
		body.put("servicesOfInterest", "Decor, catering, photography");
		body.put("themeStyle", "Royal Rajasthani");
		body.put("functionsRequired", "Mehendi, Sangeet, Wedding, Reception");
		body.put("specialRequests", "Vegetarian menu only");
		body.put("message", "Please call after 6 pm");
		body.put("preferredConsultationDate", LocalDate.now().plusWeeks(2).toString());
		return body;
	}

	private static String json(Map<String, Object> body) {
		return JSON.writeValueAsString(body);
	}

	private void stubSaveAssigningId(long id) {
		when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(invocation -> {
			Enquiry enquiry = invocation.getArgument(0);
			enquiry.setId(id);
			enquiry.setCreatedAt(SAVED_AT);
			enquiry.setUpdatedAt(SAVED_AT);
			return enquiry;
		});
	}

}
