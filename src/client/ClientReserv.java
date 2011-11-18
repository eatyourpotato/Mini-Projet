package client;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ClientReserv extends ClientG {

    public ClientReserv(String name, String host, int port) {
        super(name, host, port);
    }

    public void ajouterClient()  {
        Scanner sc = new Scanner(System.in);
        
        System.out.println("Veuillez saisir les informations sur le client");
        System.out.print("Nom : ");
        String nom = sc.nextLine();
        
        System.out.print("Prénom : ");
        String prenom = sc.nextLine();
        
        // Envoyer les informations au serveur
        // TODO
        // Construire le hashmap
        HashMap<Object, Object> mess = new HashMap<Object, Object>();
        mess.put("commande", "nouveau client");
        mess.put("nom", nom);
        mess.put("prenom", prenom);
        
        socketClient.sendMessage(mess);
        
        
    }
    
    
    
    public static void main(String[] args) {
        ClientReserv client = new ClientReserv("Client réservation", "localhost", 6000);
        
        client.run();
    }
}
