package com.wams.enquiry;

import com.wams.enquiry.dto.LinkPrimaryClientRequest;
import com.wams.entity.Client;
import com.wams.entity.User;
import com.wams.entity.WeddingProject;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.ClientRepository;
import com.wams.repository.UserRepository;
import com.wams.repository.WeddingProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Owns automatic and explicit primary-client association for enquiries. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrimaryClientAssociationService {

    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final WeddingProjectRepository projectRepository;
    private final EnquiryAccessService accessService;

    /**
     * Resolves the authoritative primary client while the caller already holds
     * the enquiry write lock. Existing explicit links win; otherwise the
     * normalized enquiry email is matched to exactly one persisted User.
     */
    @Transactional
    public Client resolveForConversion(Enquiry enquiry, User actor) {
        if (enquiry.getPrimaryClient() != null) {
            return requireValidPrimaryClient(enquiry.getPrimaryClient());
        }

        String normalizedEmail = normalizeEmail(enquiry.getEmail());
        List<User> matches = userRepository.findAllByNormalizedEmailForUpdate(normalizedEmail);
        if (matches.isEmpty()) {
            throw new BusinessRuleException(
                    "No registered primary client matches enquiry email " + normalizedEmail +
                            "; an admin must link an existing client before conversion");
        }
        if (matches.size() > 1) {
            throw new DuplicateResourceException(
                    "Multiple user accounts match normalized enquiry email " + normalizedEmail +
                            "; resolve the duplicate accounts before conversion");
        }

        User user = matches.get(0);
        if (user.getRole() != Role.PRIMARY_CLIENT) {
            throw new BusinessRuleException(
                    "Account matching enquiry email must have PRIMARY_CLIENT role");
        }
        if (!user.isActive()) {
            throw new BusinessRuleException("Account matching enquiry email is inactive");
        }

        Client client = clientRepository.findByUserId(user.getId())
                .orElseGet(() -> clientRepository.saveAndFlush(Client.builder().user(user).build()));
        link(enquiry, client, ClientLinkSource.AUTO_EMAIL, actor);
        enquiryRepository.save(enquiry);
        return client;
    }

    /**
     * Admin compare-and-set workflow. For converted enquiries, the linked
     * project's primary client is updated in the same transaction. Partner
     * ownership is never changed.
     */
    @Transactional
    public Enquiry linkByAdmin(Long enquiryId, LinkPrimaryClientRequest request) {
        User actor = accessService.currentStaff();
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN may link a primary client");
        }
        Enquiry enquiry = accessService.requireAccessibleForUpdate(enquiryId);
        Client target = requireValidPrimaryClient(clientRepository.findById(request.primaryClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Primary client not found: " + request.primaryClientId())));
        WeddingProject project = projectRepository.findByEnquiryIdForUpdate(enquiryId).orElse(null);
        if (enquiry.getStatus() == EnquiryStatus.CONVERTED && project == null) {
            throw new BusinessRuleException(
                    "Converted enquiry has no linked project; reconcile project provenance before relinking client");
        }

        Client current = enquiry.getPrimaryClient() != null
                ? enquiry.getPrimaryClient()
                : project == null ? null : project.getPrimaryClient();
        Long currentId = current == null ? null : current.getId();
        if (!Objects.equals(currentId, target.getId())) {
            if (!Objects.equals(currentId, request.expectedCurrentPrimaryClientId())) {
                throw new DuplicateResourceException(
                        "Primary client association changed; reload the enquiry before relinking");
            }
        }

        if (project != null && project.getPartnerClient() != null
                && project.getPartnerClient().getId().equals(target.getId())) {
            throw new BusinessRuleException("Primary client cannot also be the partner client");
        }

        link(enquiry, target, ClientLinkSource.ADMIN, actor);
        if (project != null) {
            project.setPrimaryClient(target);
            projectRepository.save(project);
        }
        return enquiryRepository.save(enquiry);
    }

    public Client requireValidPrimaryClient(Client client) {
        if (client.getUser() == null || client.getUser().getRole() != Role.PRIMARY_CLIENT) {
            throw new BusinessRuleException("Linked client must belong to a PRIMARY_CLIENT account");
        }
        if (!client.getUser().isActive()) {
            throw new BusinessRuleException("Linked primary client account is inactive");
        }
        return client;
    }

    private void link(Enquiry enquiry, Client client, ClientLinkSource source, User actor) {
        enquiry.setPrimaryClient(client);
        enquiry.setClientLinkSource(source);
        enquiry.setClientLinkedBy(actor);
        enquiry.setClientLinkedAt(Instant.now());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
