package com.wams.service;

import com.wams.dto.request.CreateClientRequest;
import com.wams.dto.request.UpdateClientRequest;
import com.wams.dto.response.ClientResponse;
import com.wams.entity.Client;

import java.util.List;

public interface ClientService {
    ClientResponse create(CreateClientRequest request);
    ClientResponse getById(Long id);
    Client getEntityById(Long id);
    List<ClientResponse> list();
    ClientResponse update(Long id, UpdateClientRequest request);
}
