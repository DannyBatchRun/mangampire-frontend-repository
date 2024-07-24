package org.altervista.mangampire.service;

import org.altervista.mangampire.model.Card;
import org.altervista.mangampire.model.Client;
import org.altervista.mangampire.model.SearchManga;
import org.altervista.mangampire.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FrontendServiceImpl implements FrontendService {

    @Autowired
    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendService.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private Boolean sessionLogged;
    @Override
    public boolean getSessionLogged() {
        return sessionLogged;
    }
    @Override
    public void setSessionLogged(boolean sessionLogged) {
        this.sessionLogged = sessionLogged;
    }
    @Override
    public String doLoginRequest(EndpointRequest backendRequest, RequestLogin requestLogin) {
        String page = "index";
        try {
            backendRequest.setRequest("/client/login");
            String loginUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
            String response = restTemplate.postForObject(loginUrl, requestLogin, String.class);
            if(response.equalsIgnoreCase("Login Succeded")) {
                page = "dashboard";
                setSessionLogged(true);
            } else {
                backendRequest.setRequest("/client/search");
                String searchClientUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
                String responseClient = restTemplate.postForObject(searchClientUrl, requestLogin, String.class);
                if(responseClient.equalsIgnoreCase("Client Found")) {
                    page = "loginfailed";
                } else {
                    page = "loginfailedarchive";
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error during login request", e);
            page = "error";
        }
        return page;
    }

    @Override
    public Client getClientfromBackendService(EndpointRequest backendRequest, RequestLogin requestLogin) {
        Client client = null;
        try {
            backendRequest.setRequest("/client/login/search");
            String getClientUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
            client = restTemplate.postForObject(getClientUrl, requestLogin, Client.class);
        } catch (Exception e) {
            LOGGER.error("Error during client retrieval", e);
        }
        return client;
    }
    @Override
    public String getCardsOfClientFromBackend(EndpointRequest backendRequest, long idCardHolder) {
        backendRequest.setRequest("/client/cards/?idCardHolder=" + idCardHolder);
        String cardsUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
        return restTemplate.getForObject(cardsUrl, String.class);
    }

    @Override
    public String getCartClientFromBackend(EndpointRequest backendRequest, Client client) {
        backendRequest.setRequest("/cart/search");
        SearchClient clientSearch = new SearchClient();
        clientSearch.setIdClient(client.getIdClient());
        clientSearch.setEmail(client.getEmail());
        clientSearch.setName(client.getName());
        clientSearch.setSurname(client.getSurname());
        clientSearch.setDateBirth(client.getDateBirth());
        String sendRequest = backendRequest.getEndpoint() + backendRequest.getRequest();
        return restTemplate.postForObject(sendRequest, clientSearch, String.class);
    }
    @Override
    public String addCardfromBackend(EndpointRequest backendRequest, RequestLogin requestLogin, Card card) {
        String page = "index";
        Client client = getClientfromBackendService(backendRequest, requestLogin);
        backendRequest.setRequest("/client/card/add?idClient=" + client.getIdClient());
        String cardAdditionUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
        Boolean isAdded = restTemplate.postForObject(cardAdditionUrl, card, Boolean.class);
        if(Boolean.TRUE.equals(isAdded)) {
            page = "cardadded";
        } else {
            page = "carderror";
        }
        return page;
    }

    @Override
    public ShoppingCart setShoppingCart(EndpointRequest backendRequest, RequestLogin clientCached, SearchManga manga) {
        Client client = getClientfromBackendService(backendRequest, clientCached);
        SearchClient clientSearch = new SearchClient();
        clientSearch.setIdClient(client.getIdClient());
        clientSearch.setEmail(client.getEmail());
        clientSearch.setName(client.getName());
        clientSearch.setSurname(client.getSurname());
        clientSearch.setDateBirth(client.getDateBirth());
        return new ShoppingCart(clientSearch,manga);
    }
    @Override
    public String setAdditionCartRequestInBackend(EndpointRequest backendRequest, ShoppingCart shoppingCart) {
        String page = "index";
        backendRequest.setRequest("/cart/add");
        String urlAddCart = backendRequest.getEndpoint() + backendRequest.getRequest();
        String response = restTemplate.postForObject(urlAddCart, shoppingCart, String.class);
        if (response.contains("Added manga on shopping cart") ||
                response.contains("Client have already at least one of this manga") ||
                response.contains("Client not have a shopping cart. Created.")) {
            page = "negozioadded";
        }
        return page;
    }

    @Override
    public String askBackendToCompleteTransaction(EndpointRequest backendRequest, RequestLogin loginCached, String cardNumber) {
        SearchClient client = getClientAndConvertToClientSearch(backendRequest, loginCached);
        backendRequest.setRequest("/transaction/complete?cardNumber=" + cardNumber);
        String completeTransactionUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
        return restTemplate.postForObject(completeTransactionUrl, client, String.class);
    }
    @Override
    public Boolean askBackendToAddClient(EndpointRequest backendRequest, Client client) {
        backendRequest.setRequest("/client/add");
        String clientAdditionUrl = backendRequest.getEndpoint() + backendRequest.getRequest();
        return restTemplate.postForObject(clientAdditionUrl, client, Boolean.class);
    }
    @Override
    public String getPageDependingCartClient(ClientCart clientCart) {
        return (clientCart == null || clientCart.getTotalPrice() == 0.0) ? "nocart" : "payment";
    }
    @Override
    public String getDashboardOrThePage(boolean isLogged, String page) {
        return (isLogged) ? "dashboard" : page;
    }
    private boolean controlIfClientIsOnDatabase(RequestLogin requestLogin) {
        return false;
    }

    private SearchClient getClientAndConvertToClientSearch(EndpointRequest backendRequest, RequestLogin clientCached) {
        Client client = getClientfromBackendService(backendRequest, clientCached);
        SearchClient clientSearch = new SearchClient();
        clientSearch.setIdClient(client.getIdClient());
        clientSearch.setEmail(client.getEmail());
        clientSearch.setName(client.getName());
        clientSearch.setSurname(client.getSurname());
        clientSearch.setDateBirth(client.getDateBirth());
        return clientSearch;
    }

}
