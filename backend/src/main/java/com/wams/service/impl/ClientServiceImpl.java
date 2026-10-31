package com.wams.service.impl;

import com.wams.dto.request.CreateClientRequest;
import com.wams.dto.request.UpdateClientRequest;
import com.wams.dto.response.ClientResponse;
import com.wams.entity.Client;
import com.wams.entity.User;
import com.wams.enums.Role;
import com.wams.exception.BusinessRuleException;
import com.wams.exception.DuplicateResourceException;
import com.wams.exception.ResourceNotFoundException;
import com.wams.repository.ClientRepository;
import com.wams.repository.UserRepository;
import com.wams.service.ClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public ClientServiceImpl(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ClientResponse create(CreateClientRequest r) {
        User user = userRepository.findById(r.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + r.userId()));
        if (user.getRole() != Role.PRIMARY_CLIENT && user.getRole() != Role.PARTNER_CLIENT) {
            throw new BusinessRuleException(
                    "Client record requires a user with PRIMARY_CLIENT or PARTNER_CLIENT role");
        }
        if (clientRepository.existsByUserId(user.getId())) {
            throw new DuplicateResourceException("Client already exists for userId=" + user.getId());
        }
        Client c = Client.builder()
                .user(user)
                .phoneNumber(r.phoneNumber())
                .address(r.address())
                .build();
        return ClientResponse.from(clientRepository.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getById(Long id) {
        return ClientResponse.from(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Client getEntityById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> list() {
        return clientRepository.findAll().stream().map(ClientResponse::from).toList();
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, UpdateClientRequest r) {
        Client c = getEntityById(id);
        if (r.phoneNumber() != null) c.setPhoneNumber(r.phoneNumber());
        if (r.address() != null) c.setAddress(r.address());
        return ClientResponse.from(c);
    }
}
