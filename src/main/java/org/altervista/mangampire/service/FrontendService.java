package org.altervista.mangampire.service;

import org.altervista.mangampire.model.*;
import org.altervista.mangampire.model.SearchManga;
import org.altervista.mangampire.dto.*;

public interface FrontendService {
    boolean getSessionLogged();
    void setSessionLogged(boolean sessionLogged);
    Client getClientfromBackendService(EndpointRequest backendRequest, RequestLogin requestLogin);
    String getDashboardOrThePage(boolean isLogged, String page);
    String doLoginRequest(EndpointRequest backendRequest, RequestLogin requestLogin);
    String addCardfromBackend(EndpointRequest backendRequest, RequestLogin requestLogin, Card card);
    String getCardsOfClientFromBackend(EndpointRequest backendRequest, long idCardHolder);
    String getCartClientFromBackend(EndpointRequest backendRequest, Client client);
    ShoppingCart setShoppingCart(EndpointRequest backendRequest, RequestLogin clientCached, SearchManga manga);
    String setAdditionCartRequestInBackend(EndpointRequest backendRequest, ShoppingCart shoppingCart);
    String askBackendToCompleteTransaction(EndpointRequest backendRequest, RequestLogin loginCached, String cardNumber);
    Boolean askBackendToAddClient(EndpointRequest backendRequest, Client client);
    String getPageDependingCartClient(ClientCart clientCart);

}
