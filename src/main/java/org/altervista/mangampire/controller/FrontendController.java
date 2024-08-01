package org.altervista.mangampire.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.altervista.mangampire.model.Client;
import org.altervista.mangampire.model.*;
import org.altervista.mangampire.dto.SearchManga;
import org.altervista.mangampire.dto.ClientCart;
import org.altervista.mangampire.dto.EndpointRequest;
import org.altervista.mangampire.dto.RequestLogin;
import org.altervista.mangampire.dto.SearchClientManga;
import org.altervista.mangampire.service.FrontendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Controller
public class FrontendController {
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private EndpointRequest backendService;
    @Autowired
    private FrontendService service;
    @Autowired
    private RequestLogin loginCached;
    @Autowired
    private Map<String, ClientCart> clientCartCached;
    @PostConstruct
    public void init() {
        System.out.println("Initializing localhost for Patch Request as default.");
        backendService.setEndpoint("http://localhost:8100");
        System.out.println("Backend Service ---> " + backendService.getEndpoint());
    }
    @GetMapping("/")
    public String getLogin(Model model) throws JsonProcessingException {
        String page = service.getDashboardOrThePage(service.getSessionLogged(),"index");
        if(!page.equals("index")) {
            tryToLoginPlatform(loginCached, model);
        }
        return page;
    }
    @PatchMapping("/backend/url")
    public ResponseEntity<String> setBackendEndpoint(@Validated @RequestBody EndpointRequest request) {
        backendService.setEndpoint("http://" + request.getEndpoint());
        return ResponseEntity.ok().body("Endpoint of Backend Setted");
    }

    @GetMapping("/register")
    public String getRegistrationForm(Model model) throws JsonProcessingException {
        String page = (!service.getSessionLogged()) ? "registrati" : "dashboard";
        if(page.equals("dashboard")) {
            tryToLoginPlatform(loginCached, model);
        }
        return page;
    }
    @PostMapping("/login")
    public String tryToLoginPlatform(@ModelAttribute RequestLogin requestLogin, Model model) throws JsonProcessingException {
        if(requestLogin.getEmail() == null || requestLogin.getPassword() == null) {
            String backToPage = (service.getSessionLogged()) ? "dashboard" : "index";
            return service.getDashboardOrThePage(service.getSessionLogged(),backToPage);
        }
        String page = service.doLoginRequest(backendService,requestLogin);
        if(page.equalsIgnoreCase("dashboard")) {
            Client clientFound = service.getClientfromBackendService(backendService, requestLogin);
            model.addAttribute("idClient", clientFound.getIdClient());
            model.addAttribute("email", clientFound.getEmail());
            model.addAttribute("name", clientFound.getName());
            model.addAttribute("surname", clientFound.getSurname());
            model.addAttribute("dateBirth", clientFound.getDateBirth());
            model.addAttribute("cardQuantity", clientFound.getCardQuantity());
            ShoppingCart shoppingCartClient = service.getCartClientFromBackend(backendService, clientFound);
            String cardsJson = service.getCardsOfClientFromBackend(backendService, clientFound.getIdClient());
            if (shoppingCartClient == null || shoppingCartClient.getManga() == null || shoppingCartClient.getManga().isEmpty()) {
                model.addAttribute("totalPrice", "0");
                model.addAttribute("shoppingCart", "Carrello Vuoto");
                ClientCart emptyClientCart = new ClientCart("",0.0);
                clientCartCached.put(clientFound.getEmail(),emptyClientCart);
            } else {
                double totalPrice = 0;
                StringBuilder formattedJson = new StringBuilder();
                for (Manga manga : shoppingCartClient.getManga()) {
                    String name = manga.getName();
                    int volume = manga.getVolume();
                    int quantity = manga.getQuantity();
                    double price = manga.getPrice();
                    formattedJson.append("Nome: ").append(name).append(", Volume: ").append(volume).append(", Quantità: ").append(quantity).append("<br />");
                    totalPrice += price * quantity;
                }
                model.addAttribute("shoppingCart", formattedJson.toString());
                model.addAttribute("totalPrice", String.format("%.2f", totalPrice));
                ClientCart clientCart = new ClientCart(formattedJson.toString(),totalPrice);
                clientCartCached.put(clientFound.getEmail(),clientCart);
            }

            List<Card> cardList = mapper.readValue(cardsJson, new TypeReference<List<Card>>() {});
            StringBuilder formattedCards = new StringBuilder();
            for (Card card : cardList) {
                String cardNumber = card.getCardNumber();
                String censoredCardNumber = "";
                censoredCardNumber = "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
                formattedCards.append("Numero Carta: ").append(censoredCardNumber).append("<br />");
            }
            if(formattedCards.toString().isEmpty()) {
                model.addAttribute("cards", "Nessuna Carta");
            } else {
                model.addAttribute("cards", formattedCards.toString());
            }
        }
        loginCached.setEmail(requestLogin.getEmail());
        loginCached.setPassword(requestLogin.getPassword());
        return page;
    }
    @GetMapping("/logout")
    public String doLogoutFromThePlatform () {
        if(service.getSessionLogged()) {
            service.setSessionLogged(false);
            loginCached.reset();
        }
        return "redirect:/";
    }

    @GetMapping("/card/register")
    public String getPageOfCardRegister(Model model) {
        String page = "";
        page = (service.getSessionLogged()) ? "addcard" : "index";
        model.addAttribute("email",loginCached.getEmail());
        return page;
    }
    @PostMapping("/return")
    public String returnToDashboard (Model model) throws JsonProcessingException {
        String page = "";
        if(service.getSessionLogged()) {
            page = tryToLoginPlatform(loginCached, model);
        } else {
            page = "index";
        }
        return page;
    }

    @GetMapping("/shop")
    public String goToTheShop (Model model) {
        String page = "";
        page = (service.getSessionLogged()) ? "negozio" : "index";
        model.addAttribute("email",loginCached.getEmail());
        return page;
    }

    @PostMapping("/cart/add")
    public String addMangaToCart(@ModelAttribute SearchManga manga, Model model) {
        String page = "";
        if(service.getSessionLogged()) {
            model.addAttribute("email", loginCached.getEmail());
            SearchClientManga searchClientManga = service.setShoppingCart(backendService, loginCached, manga);
            page = service.setAdditionCartRequestInBackend(backendService, searchClientManga);
        } else if (!service.getSessionLogged()) {
            page = "index";
        }
        return page;
    }
    @PostMapping("/card/submit")
    public String requestAddCardToBackEnd(@ModelAttribute Card card, Model model) {
        return service.addCardfromBackend(backendService,loginCached,card);
    }
    @GetMapping("/transaction/get")
    public String getThePageOfTransaction(Model model) throws JsonProcessingException {
        if(service.getSessionLogged()) {
            model.addAttribute("email", loginCached.getEmail());
            ClientCart clientCart = clientCartCached.get(loginCached.getEmail());
            model.addAttribute("totalPrice", clientCart.getTotalPrice());
            Client clientFound = service.getClientfromBackendService(backendService, loginCached);
            String cardsJson = service.getCardsOfClientFromBackend(backendService, clientFound.getIdClient());
            List<Card> cardList = mapper.readValue(cardsJson, new TypeReference<List<Card>>() {});
            Set<String> formattedCardsAbsolute = new TreeSet<>();
            Set<String> formattedCardsComplete = new TreeSet<>();
            for (Card card : cardList) {
                formattedCardsAbsolute.add(card.getCardNumber());
                formattedCardsComplete.add(card.getCardNumber() + " - " + card.getCardHolderName() + " " + card.getCardHolderSurname());
            }
            model.addAttribute("cards", formattedCardsAbsolute);
            model.addAttribute("cardsComplete", formattedCardsComplete);
            if(clientCart.getCartFormatted().isEmpty() && clientCart.getTotalPrice() == 0.0) {
                return "nocart";
            } else {
                return "payment";
            }
        } else {
            return "index";
        }
    }
    @PostMapping("/transaction/submit")
    public String completeTransactionWithFrontend(@ModelAttribute("cardSelection") String cardNumber, @ModelAttribute Shipment shipment, Model model) {
        String page = "index";
        String message = "none";
        if(service.getSessionLogged()) {
            model.addAttribute("email", loginCached.getEmail());
            String responseBuy = service.askBackendToCompleteTransaction(backendService,loginCached,cardNumber);
            if(responseBuy.contains("NOT FOUND")) {
                page = "paymentfailed";
                message = "Uno o più manga risultano non disponibili. Per favore, riprova tra qualche giorno.";
                model.addAttribute("shipment", "Torna alla Dashboard");
            } else if (responseBuy.contains("PAYMENT REQUIRED")) {
                page = "paymentfailed";
                message = "La Carta non ha un importo sufficiente per continuare. Per favore, seleziona un altra carta.";
                model.addAttribute("shipment", "Torna alla Dashboard");
            } else if (responseBuy.contains("OK")) {
                page = "paymentsuccess";
                message = "Acquisto Effettuato con Successo! I Manga verranno spediti ai seguenti dati :\n";
                String formattedShipment = "<h3>" + shipment.getName() + " " + shipment.getSurname() + "</h3>" + shipment.getAddress() + "<br />" + shipment.getPostalCode() + " " + shipment.getCity();
                model.addAttribute("shipment", formattedShipment);
            }
            model.addAttribute("message", message);
        } else {
            return "index";
        }
        return page;
    }
    @PostMapping("/register/submit")
    public String addClientToDatabase(@ModelAttribute Client client, Model model) throws JsonProcessingException {
        String page = "dashboard";
        if(!service.getSessionLogged()) {
            String message = "No message";
            Boolean isClientAdded = service.askBackendToAddClient(backendService, client);
            if (Boolean.TRUE.equals(isClientAdded)) {
                message = "Registrazione avvenuta! NON CONDIVIDERE LA PASSWORD CON NESSUNO! Puoi effettuare il login con i seguenti dati :<br />";
                String formattedRegistration = "<h3>" + client.getEmail() + "</h3>" + "Password : " + client.getPassword() + "<br />";
                page = "registrasuccess";
                model.addAttribute("registration", formattedRegistration);
            } else {
                message = "Qualcosa è andato storto, probabilmente l'indirizzo e-mail risulta già registrato. Riprova.<br />";
                model.addAttribute("registration", "Registrazione non riuscita.");
                page = "registrafailed";
            }
            model.addAttribute("message", message);
        } else {
            tryToLoginPlatform(loginCached, model);
        }
        return page;
    }
    @GetMapping("/cart/clear")
    public String askBackendToClearCartClient(Model model) throws JsonProcessingException {
        String page = "index";
        Client clientFound = service.getClientfromBackendService(backendService, loginCached);
        backendService.setRequest("/cart/clear?idClient=" + clientFound.getIdClient());
        Boolean cleared = service.clearCartClient(backendService);
        if(Boolean.TRUE.equals(cleared)) {
            page = tryToLoginPlatform(loginCached, model);
            ClientCart clientCart = new ClientCart("",0.0);
            clientCartCached.put(clientFound.getEmail(),clientCart);
        } else {
            page = "nocart";
        }
        return page;
    }

}
