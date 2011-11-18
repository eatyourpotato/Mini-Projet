package serveur;

//
// IUT de Nice / Departement informatique / Module APO-Java
// Annee 2009_2010 - Composants reseaux generiques sous TCP/IP
//
// Classe ServeurG_TCP - Echanges parametrables de messages avec des clients IP
//                       au moyen d'une socket unique dediee (TP_5)
//                              
// Edition "Draft" : reception de messages en mode caracteres 
//                              
//    + Version 0.0.0	: version initiale avec un seul client possible et une
//                        capacite limitee a la reception d'un seul message
//
//    + Version 0.2.0   : ajout attribut "statusReception" pour telecommander  
//                        le debut et la fin de reception des messages
//                        + accesseurs debuterReception et stopperReception
//                        + modification induite de la methode run
//
// Edition A       : echanges bidirectionnels de messages en mode "bytes" 
//
//    + Version 1.0.0   : modification des attributs et de la methode "iniflux" 
//                        pour gerer la reception par serialisation d'objets 
//                        dans la socket sous jacente (Ex_9)
//                       
//    + Version 1.1.0   : mise en place d'une voie de retour limitee a l'envoi 
//                        d'une reponse au premier message attendu (Ex_10)
//
// Edition B       : echanges multiples bidirectionnels de messages
//
//    + Version 2.0.0   : modification de la m�thode "run" pour que le serveur
//                        puisse gerer de multiples �changes quel qu'en soit 
//                        l'ordre
//    + Version 2.1.0   : Correction de l'emission/reception : temporisation
//     
//    + Version 2.2.0   : Correction definitive de l'emission/reception : blocage de l'emission au meme titre que la reception.
// Auteur : A. Thuaire
//

import java.util.*;
import java.io.*;
import java.net.*;
      
public class ServeurG_TCP extends Thread {
private int                portReception;
private Socket             socketSupport;
private ServerSocket       socketServeur;
private ObjectInputStream  bIn;             // Buffer entree en mode "bytes"
private ObjectOutputStream bOut;            // Buffer sortie en mode "bytes"

private LinkedList         listeEmission;   // messages a envoyer
private boolean            statusEmission;  // Status autorisation a emettre

private LinkedList         listeReception;  // messages recus
private boolean            statusReception; // Status autorisation a recevoir
  
// ------                                      *** Premier constructeur normal  
   
   public ServeurG_TCP (String nomThread, ServerSocket socket) {
      
      super(nomThread);
      portReception  = 8080;
      socketServeur = socket;
      listeEmission  = new LinkedList();
      statusEmission = false;
      listeReception = new LinkedList();
      statusReception= false;
      
      start();
   }
   
// ------                                       *** Second constructeur normal  
   
   public ServeurG_TCP (String nomThread, int port) {
   	
      super(nomThread);
      portReception  = port;

      listeEmission  = new LinkedList();
      statusEmission = false;

      listeReception = new LinkedList();
      statusReception= false;
   	 
      start();
   }

// ------                                                      *** Accesseurs  
   
   public Socket     obtenirSocket  ()         {return socketSupport;}

//                                                       --- Partie reception 

   public LinkedList obtenirMessagesRecus()    {return listeReception;}
   public boolean    obtenirStatusReception () {return statusReception;}
    
   public HashMap    retirerMessage ()  {
   HashMap msg= null;
   	
      if (listeReception.size() == 0) return null;
   	  
      // Executer une operation atomique pour obtenir le premier
      // message courant recu et le retirer de la liste
      //
      synchronized (listeReception) {
   	  	
         msg= (HashMap)listeReception.getFirst();
   	  
         listeReception.removeFirst();
      }
      
      // Restituer le resultat
      //
      return msg;
   }
   
   public void       debuterReception() {statusReception= true; }
   public void       stopperReception() {statusReception= false;}

//                                                         --- Partie emission 

   public void       ajouterMessage(HashMap msg) {listeEmission.add(msg);}
   public LinkedList obtenirMessagesEmis()       {return listeEmission;}
   public String     obtenirMessage() {
   	
      if (listeEmission.size() == 0) return null;
   	  return (String)listeEmission.getFirst();
   }
   public void       debuterEmission() {statusEmission= true; }
   public void       stopperEmission() {statusEmission= false;}  
   
// ------                                                     *** Methode main  

   public static void main(String[] args) {
   	
      // Creer et demarrer un serveur IP
      //
      ServerSocket serveur = null;
      try {serveur= new ServerSocket(8080);} catch(Exception e) {}
      ServeurG_TCP socket_j1= new ServeurG_TCP("j1", serveur);
      ServeurG_TCP socket_j2= new ServeurG_TCP("j2", serveur);
      int i = 0;
      boolean mutex = false;
      System.out.print("* Creation et demarrage du serveur TCP/IP ");
      System.out.println("(V 2.0.0)\n");
      	   
      // Autoriser la reception des messages
      //
      
      // Attendre les messages en provenance du client unique
      //
      HashMap msg= null;
      HashMap reponse= null;

      	 // Attendre la reception d'un nouveau message
      	 //
	 socket_j1.debuterReception();
      	 while (socket_j1.obtenirMessagesRecus().size() == 0) 
	 	Chrono.attendre(100);
      	 // Retirer le message courant de la liste de reception
      	 //
      	 msg= socket_j1.retirerMessage();

	 socket_j1.debuterEmission();
	 socket_j1.stopperReception();
      	 
      	 // Visualiser la commande recue
         // 
         System.out.print ("<-- Commande recue : ");
         System.out.println ((String)msg.get("commande"));
	 
	 // Attendre la reception d'un nouveau message
      	 //
	       
         socket_j2.debuterReception();
      	 while (socket_j2.obtenirMessagesRecus().size() == 0) 
	 Chrono.attendre(100);
      	 
      	 // Retirer le message courant de la liste de reception
      	 //
      	 msg= socket_j2.retirerMessage();
         socket_j2.debuterEmission();
	 socket_j2.stopperReception();
      	 
      	 // Visualiser la commande recue
         // 
         System.out.print ("<-- Commande recue : ");
         System.out.println ((String)msg.get("commande"));

	
         
         mutex = true;
	 
	 while(i<255){
		
		if (mutex){
			reponse= new HashMap();
			reponse.put("commande", "ENVOYER");
			socket_j1.ajouterMessage(reponse);
			socket_j1.debuterEmission();
			socket_j1.stopperReception();
			socket_j1.stopperEmission();
			socket_j1.debuterReception();
			while (socket_j1.obtenirMessagesRecus().size() == 0){
				System.out.print(".");
				Chrono.attendre(100);
			}
			msg= socket_j1.retirerMessage();
			System.out.print ("<-- Message recu : ");
			System.out.println ("Message " + i + " : " + (String)msg.get("message"));
			reponse= new HashMap();
			reponse.put("commande", "AFFICHER");
			reponse.put("message", msg.get("message"));
			socket_j2.ajouterMessage(reponse);
			socket_j2.debuterEmission();
			socket_j2.stopperReception();
			Chrono.attendre(1000);
		}
		else {
			reponse= new HashMap();
			reponse.put("commande", "ENVOYER");
			socket_j2.ajouterMessage(reponse);
			socket_j2.debuterEmission();
			socket_j2.stopperReception();
			socket_j2.stopperEmission();
			socket_j2.debuterReception();
			while (socket_j2.obtenirMessagesRecus().size() == 0){
				System.out.print(".");
				Chrono.attendre(100);
			}
			msg= socket_j2.retirerMessage();
			System.out.print ("<-- Message recu : ");
			System.out.println ("Message " + i + " : " + (String)msg.get("message"));
			reponse= new HashMap();
			reponse.put("commande", "AFFICHER");
			reponse.put("message", msg.get("message"));
			socket_j1.ajouterMessage(reponse);
			socket_j1.debuterEmission();
			socket_j1.stopperReception();
			Chrono.attendre(1000);
		
		}
		if (mutex) mutex = false; else mutex = true;
		i++;
		Chrono.attendre(1000);
	
	}
	 reponse = new HashMap();
	 reponse.put("commande", "TERMINE");
	 socket_j1.ajouterMessage(reponse);
	 socket_j2.ajouterMessage(reponse);
	 Chrono.attendre(5000);
	 System.out.print("fermeture du serveur\n");
         // Interdire l'emission de messages
         //
         socket_j1.stopperEmission();
         socket_j2.stopperEmission();
      
      // Stopper l'emission/reception des messages
      //
         socket_j1.stopperReception();
         socket_j2.stopperReception();
      
      // Fermer les flux d'echange avec le client unique
      //
         socket_j1.fermer();
         socket_j2.fermer();		
   }  
   
// ------                                                      *** Methode run  

   public void run() {
   
      // Etablir la connexion avec le serveur cible
      //
      accepter();
      
      // Tant que l'instance de ServeurG_TCP est active
		//
		while (true)
		{
			// Attendre l'autorisation d'emettre ou de recevoir
			//
			while (!(statusEmission || statusReception)) 
				Chrono.attendre(200);

			// Si on a une autorisation � emettre
			//
			Chrono.attendre(1000);
			if (statusEmission) 
			{
				
				// Tant que la liste d'emission n'est pas remplie
				//
				
				while (listeEmission.size() == 0){
					Chrono.attendre(200);				
				}
				
					// Executer une operation atomique d'envoi d'un message 
					//
					synchronized (listeEmission){
						// D�clarer un dictionnaire h�bergeur
						//
						HashMap<Object, Object> msg= null;

						// Extraire le message suivant
						//
						msg= (HashMap<Object, Object>)listeEmission.getFirst();

						// Envoyer le message courant
						//
						statusEmission= envoyerMessage(msg);

						// Retirer ce message de la liste
						//
						listeEmission.removeFirst();
					}
				
			}

			// Si on a une autorisation � recevoir
			//
			if (statusReception) 
				// Attendre un message -> Le mettre dans la liste de messages recus (bloquant)
				attendreMessage();
		}


	}
  
// ------                                                 *** Methode accepter  

   private boolean accepter() {
   
      
      // Attendre la connexion du client
      //
      try{socketSupport= socketServeur.accept();}
      catch (Exception e){return false;}
      
      return initFlux(socketSupport);
   }
   
// ------                                                 *** Methode initFlux  

   private boolean initFlux(Socket s) {
   
      // Controler l'existence de la socket support
      //
      if (s==null) return false;
   
      // Creer le flux de sortie
      //
      OutputStream streamOut= null;
      try{streamOut= s.getOutputStream();}
      catch(Exception e){return false;}
      if (streamOut == null) return false;
      
      // Creer le buffer de sortie
      //
      try{bOut= new ObjectOutputStream(streamOut);}
      catch (Exception e) {return false;}
      if (bOut == null) return false;
      
      // Creer le flux d'entree
      //
      InputStream streamIn= null;
      try{streamIn= s.getInputStream();}
      catch(Exception e){return false;}
      if (streamIn == null) return false;

      // Creer le buffer d'entree
      //
      // ATTENTION : le constructeur est bloquant jusqu'a la reception du
      //             premier objet (message)
      //
      try{bIn= new ObjectInputStream(streamIn);}
      catch (Exception e) {return false;}
      if (bIn == null) return false;
      
      return true;
   }
   
// ---                                             *** Methode attendreMessage  

   public void attendreMessage () {
   Object msg=null;

      // ATTENTION : la methode readObject leve plusieurs types (classes)
      //             d'exceptions suivant la nature du probleme rencontre
      //
      while (true) {
         try {
            msg= bIn.readObject();
      	    if (msg != null) break;
      	 }
      	
      	 // Traiter le cas ou l'autre extremite de la socket disparait sans
      	 // coordination prealable au niveau applicatif (OSI - 7).
      	 //
      	 // Ce cas se produit quand l'objet "socket" distant est detruit
      	 // (mort du thread distant par exemple)
      	 //                     
      	 catch (SocketException e){}
      	 
      	 // Traiter le cas ou l'autre extremite ferme la socket sans 
      	 // coordination prealable au niveau applicatif (OSI - 7)
      	 //
      	 catch (EOFException e){}
      	 
      	 // Traiter le cas d'autres exceptions relatives aux IO
      	 //
      	 catch (IOException e){}
      	 
      	 // Traiter les autres cas d'exceptions
      	 //
      	 catch (Exception e){}
      	 
      	 // Temporiser pour attendre le message suivant
      	 
      	 Chrono.attendre(100);
      }
      
      // Enregistrer le message courant dans la liste des messages
      //
      listeReception.add(msg);
   }

// ---                                              *** Methode envoyerMessage  

   public boolean envoyerMessage (Object msg) {
  	
      // Controler la validite du flux de sortie
      //
      if (bOut == null) return false;
      
      // Transferer le message dans le flux de sortie
      //
      try {bOut.writeObject(msg);}
   	  catch(Exception e) {return false;}
   	
      return true;
   }
   
// ------                                                   *** Methode fermer  

   public void fermer () {
   	
   	  try { 
   	     bIn.close();
   	     bOut.close();
   	     socketSupport.close();
   	  }
   	  catch(Exception e){}
   }
   
// -------------------------------------      *** Classe interne privee Chrono
   
   public static class Chrono {

      public static void attendre (int tms) {
         	
         // Attendre tms millisecondes, en bloquant le thread courant 
         //
         try {Thread.currentThread().sleep(tms);} 
         catch(InterruptedException e){}
      }
   }
}
