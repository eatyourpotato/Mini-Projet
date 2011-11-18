package client;

//
// IUT de Nice / Departement informatique / Module APO-Java
// Annee 2010_2011 - Composants reseaux generiques sous TCP/IP
//
// Classe ClientG_TCP - Echanges parametrables de messages avec un serveur 
//                      TCP/IP au moyen d'une socket unique dediee (TP_5)
//                              
// Edition "Draft" :  emission de messages en mode caracteres
//                              
//    + Version 0.0.0	: version initiale avec creation d'un thread autonome
//
//    + Version 0.1.0   : introduction temporisation avant fermeture socket
//                        + attente d'au moins un message au demarrage
//
//    + Version 0.2.0   : modification de la methode connecter pour autoriser
//                        un demarrage du client TCP/IP avant celui du serveur
//
//    + Version 0.3.0   : ajout attribut "statusEmission" pour telecommander le 
//                        debut et la fin d'emission des messages
//                        + accesseurs debuterEmission et stopperEmission
//                        + modification induite de la methode run
//
// Edition A       : echanges bidirectionnels de messages en mode "bytes" 
//
//    + Version 1.0.0   : modification des attributs et de la methode "iniflux" 
//                        pour gerer l'emission par serialisation d'objets dans 
//                        la socket sous jacente (Ex_9)
//                       
//    + Version 1.1.0   : ajout des attributs et des methodes necessaires a la
//                        reception d'un message de retour (Ex_10) 
// 
// Edition B       : echanges multiples bidirectionnels de messages
//
//    + Version 2.0.0   : modification de la m�thode "run" pour que le client
//                        puisse gerer de multiples �changes quel qu'en soit 
//                        l'ordre
//
//    + Version 2.1.0   : Correction de l'emission/reception : temporisation
// Auteur : A. Thuaire, O. Cacciuttolo
//

import java.util.*;
import java.io.*;
import java.net.*;
@SuppressWarnings("unchecked")

public class ClientG_TCP extends Thread {

	// ------------------------------------------------------------------------------------------------------------------
	// ------									   *** Attributs de classe
	//
	private String             nomServeur;
	private int                portServeur;
	public  Socket             socketSupport;
	private ObjectInputStream  bIn;             // Buffer entree en mode "bytes"
	private ObjectOutputStream bOut;            // Buffer sortie en mode "bytes"

	private LinkedList<Object>      listeEmission;   // messages a envoyer
	private boolean            		statusEmission;  // Status autorisation a emettre

	private LinkedList<Object>      listeReception;  // messages recus
	private boolean            		statusReception; // Status autorisation a recevoir

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                      *** Premier constructeur normal  

	public ClientG_TCP (String nomThread) {

		super(nomThread);
		nomServeur     = "localhost"; 
		portServeur    = 8080;

		listeEmission  = new LinkedList<Object>();

		listeReception = new LinkedList<Object>();
		
		start();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                       *** Second constructeur normal  

	public ClientG_TCP (String nomThread, String host, int port) {

		super(nomThread);
		nomServeur     = host; 
		portServeur    = port;

		listeEmission  = new LinkedList<Object>();
		statusEmission = false;

		listeReception = new LinkedList<Object>();
		statusReception= false;

		start();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                        *** Accesseurs

	public Socket     getSocket()             {return socketSupport;}

	//                                                          --- Partie emission 

	public void       			addMessage(HashMap<Object, Object> msg) 	{listeEmission.add(msg);}
	public LinkedList<Object> 	getMessages()           					{return listeEmission;}
	public String     			getMessage() 								
	{
		if (listeEmission.size() == 0) 
			return null; 
		return (String)listeEmission.getFirst();
	}

	public void       			startEmission() 							{statusEmission= true;}
	public void       			stopEmission() 								{statusEmission= false;} 

	//                                                         --- Partie reception 

	public LinkedList<Object> 					getMessagesRecus()    		{return listeReception;}
	public boolean    							getStatusReception() 		{return statusReception;}
	public boolean    							getStatusEmission() 		{return statusEmission;}

	public HashMap<Object, Object>    			removeMessage()  
	{
		HashMap<Object, Object> msg= null;

		if (listeReception.size() == 0) return null;

		// Executer une operation atomique pour obtenir le premier
		// message courant recu et le retirer de la liste
		//
		synchronized (listeReception) 
		{
			msg= (HashMap<Object, Object>)listeReception.getFirst();
			listeReception.removeFirst();
		}

		// Restituer le resultat
		//
		return msg;
	}

	public void       startReception() {statusReception= true; }
	public void       stopReception() {statusReception= false;}

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                      *** Methode main  

	public static void main(String[] args) {

		// Creer et demarrer un client TCP
		//
		String host = "localhost";
		int port = 8080;
		ClientG_TCP clientTCP= new ClientG_TCP("ClientG", host, port);

		System.out.print("* Creation et demarrage du client TCP/IP ");
		System.out.println("V 2.0.0\n");

		// Attendre la mise en service du serveur
		//
		while (clientTCP.getSocket() == null) 
			Chrono.attendre(200);
		System.out.println("* Client connecte au serveur");
		System.out.println();

		// Obtenir la socket support
		//
		Socket socketSupport= clientTCP.getSocket();

		// Construire le message a envoyer au serveur
		//
		HashMap msg= new HashMap();

		msg.put("commande",  "DECLARER");
		msg.put("pseudo",    "ClientPRO");
		msg.put("adresseIP", socketSupport.getLocalAddress().getHostAddress());

		clientTCP.sendMessage(msg);
		
		int i=0;
		while (true){
			msg = clientTCP.waitMessage();
			if (msg.get("commande").equals("RECU")){
				msg = new HashMap();
				msg.put("commande", "BLABLA " + i);
				clientTCP.sendMessage(msg);
				i++;
				
			}
			else if (msg.get("commande").equals("TERMINE")){
				break;
			}
			else break;
		}

		// Stopper la reception des messages
		//
		clientTCP.stopReception();
		clientTCP.stopEmission();

		// Fermer les flux d'echange avec le serveur
		//
		clientTCP.fermer();

		System.out.println("* Client deconnecte");
	}  

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                      *** Methode run  

	public void run() {

		// Etablir la connexion avec le serveur cible
		//
		connecter();
		
		new ThreadRecepter().start();

		// Tant que l'instance de clientG_TCP est active
		//
		while (true)
		{

			// Si on a une autorisation � emettre
			//
			
				if (listeEmission.size() != 0){

				System.out.println("J'emets");
					
				// Executer une operation atomique d'envoi d'un message 
				//
				synchronized (listeEmission) 
				{
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
				
			

		}


	}

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                *** Methode connecter  

	private boolean connecter() {

		// Creer une connexion avec le serveur cible
		//
		while (true) {

			// Creer la socket support
			//
			try{socketSupport= new Socket(nomServeur, portServeur);}
			catch (Exception e){}

			// Controler la validite de cette socket
			//
			if (socketSupport != null) break;
		}

		// Initialiser les flux entrant et sortant de la connexion
		//
		return initFlux(socketSupport);
	}

	// ------------------------------------------------------------------------------------------------------------------
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

	// ------------------------------------------------------------------------------------------------------------------
	// ---                                              *** Methode envoyerMessage  

	public boolean envoyerMessage (Object msg) {

		// Controler la validite du flux de sortie
		//
		if (bOut == null) return false;

		// Transferer le message dans le flux de sortie
		//
		try {bOut.writeObject(msg);}
		catch(Exception e){return false;}

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------------
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
//			Chrono.attendre(200);
		}

		// Enregistrer le message courant dans la liste des messages
		//
		listeReception.add(msg);
		System.out.println("�tat HashMap apr�s reception du message : " + listeReception);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                   *** Methode fermer  

	public void fermer () {

		try { 
			bIn.close();
			bOut.close();
			socketSupport.close();
		}
		catch(Exception e){}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                   *** Methode waitMessage

	public HashMap<Object, Object> waitMessage() {

		// Autoriser la reception de messages
		//
		startReception();

		// Attendre la reponse du serveur
		//
		while (getMessagesRecus().size() == 0)
			Chrono.attendre(100);

		// Retirer la reponse du serveur
		//
		HashMap<Object, Object> reponse= removeMessage();

		// Stopper la reception des messages
		//
		stopReception();
		Chrono.attendre(100);
		
		// Visualiser la commande recue
		// 
		System.out.print("<-- Commande recue : ");
		System.out.println((String) reponse.get("commande"));

		// retourner la reponse
		//
		return reponse;
	}
	
	// ------------------------------------------------------------------------------------------------------------------
	// ------                                                   *** Methode sendMessage
	
	public Boolean sendMessage(HashMap<Object, Object> H) {
		
		// Ajouter le message courant a la liste des messages a envoyer
		//
		addMessage(H);
		
		// Autoriser l'�mission des messages pendant 500ms
		//
		Chrono.attendre(100); // <-- �tre s�r que le serveur est en attente du message.
		startEmission();
		Chrono.attendre(500);
		stopEmission();
		
		// Visualiser la commande transmise
		//
		System.out.println("--> Commande envoyee  : " + "DECLARER");
		
		return true;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// -------------------------------------      *** Classe interne privee Chrono     ----------------------------------
	// ------------------------------------------------------------------------------------------------------------------

	public static class Chrono {

		public static void attendre (int tms) {

			// Attendre tms millisecondes, en bloquant le thread courant 
			//
			try {Thread.currentThread();
			Thread.sleep(tms);} 
			catch(InterruptedException e){}
		}
	}
	
	public class ThreadRecepter extends Thread {
		
		public void run(){
			while (true){
			System.out.println("Je re�ois");
			// Attendre un message -> Le mettre dans la liste de messages recus (bloquant)
			attendreMessage();
			}
		}
		
	}
}
