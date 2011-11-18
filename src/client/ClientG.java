package client;

public class ClientG {
    
    protected ClientG_TCP socketClient;

    public ClientG(String name, String host, int port)   {
        socketClient = new ClientG_TCP(name, host, port);
        
        
    }
    
    public void run()  {
        
    }

}
