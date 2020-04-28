package assegnamento2;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class NodeImpl extends UnicastRemoteObject
{

	private static final int PORT = 1099;
    private static final int MINTIME = 100;
    private static final int MAXTIME = 500;
  
	private static int ID;
	private Registry registry;
	private Random random;
	
	// elezione
	private Node coordinator;
	private boolean startNewElection;
	
	// mutex
	private List<Integer> requests;
	private Node user;
	private boolean canUse;
	
	// probabilità
	private static final int H = 10;
	
	public enum State
	  {
		RUNNING,
	    FAILED;
	  }
	
	private State state;
	
	// Costruttore
	public NodeImpl(int i) throws RemoteException
	{
		this.ID = i;
		this.random = new Random();
		this.startNewElection = false;
		this.coordinator = null;
		this.requests = new ArrayList<>();
		this.user = null;
		this.canUse = false;
	}
	
	
	// Getters e metodi di interfaccia
	
	public int getID() throws RemoteException
	{
		return this.ID;
	}
	
	public State getState() throws RemoteException
	{
		return this.state;
	}
	
	public void setCoordinator(int id) throws RemoteException, NotBoundException
	{
		try {
			this.coordinator = (Node) this.registry.lookup(Integer.toString(id));
		} catch (RemoteException e) {
			 e.printStackTrace();
		}
		return;
	}
	
	
	// Metodi privati del Nodo
	
	private Registry registryInitialization() throws RemoteException
	{
		Registry reg = null;
		try {
			reg = LocateRegistry.createRegistry(PORT);
		} catch (Exception ex){
			try {
				reg = LocateRegistry.getRegistry(PORT);
			} catch (RemoteException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		return reg;
	}
	
	private void register() throws RemoteException
	{
		Node stub = null;
		try {
			stub = (Node) UnicastRemoteObject.exportObject(this, 0);
			this.registry.rebind(Integer.toString(this.ID), stub);
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}
	
	
	/*
	 A “running” node can move in a “failed” state with probability H. A “failed” node can move in the “running state” 
	 with probability K. To simulate this behavior, a node flips repeatedly its “change state” coin with a random delay 
	 between two successive flips. Of course, when a node moves to the “running” state, then it starts a new election. 
	 */
	private void liveOrDie() throws RemoteException
	{
		int randomNumber = this.random.nextInt(100) + 1; // range [1, 100]
		
		switch(this.state)
		{
			case FAILED:
				
				if (randomNumber >= H)
				{
					System.out.println("Nodo " + Integer.toString(this.ID) + "è tornato in funzione");
					startElection();
				}
				break;
			
			case RUNNING:
				
				if (randomNumber < H)
				{
					this.state = State.FAILED;
					System.out.println("Failure nodo " + Integer.toString(this.ID));
				}
				break;
		}
			
	}
	
	// Interface method
	private void startElection() throws RemoteException
    {
		this.startNewElection = true;
	}
	
	// Bully algorithm
	private void election() throws RemoteException, NotBoundException
	{
		System.out.println("Nodo " + this.ID + "richiede una nuova elezione");

		boolean anybodyAnswered = false;
		
		List<String> nodeIDs = Arrays.asList(this.registry.list());

	    for (String id : nodeIDs)
	    {
	    	Node stub = (Node) this.registry.lookup(id);
	    	if (this.ID < stub.getID() && stub.getState() != State.FAILED)
	    	{
		    	try {
			    	stub.startElection();
			    	anybodyAnswered = true;
				} catch (RemoteException e){
					 e.printStackTrace();
				}
	    	}
	    }

		if (!anybodyAnswered) {
			this.sendVictoryMessage();
		}
	}
	
	private void sendVictoryMessage() throws RemoteException, NotBoundException
	{
		System.out.println("Nodo " + this.ID + "è il nuovo coordinatore");
		List<String> nodeIDs = Arrays.asList(this.registry.list());

	    for (String id : nodeIDs)
	    {
	    	Node stub = (Node) this.registry.lookup(id);
	    	try {
		    	stub.setCoordinator(this.ID);
			} catch (RemoteException e) {
				 e.printStackTrace();
			}
	    }
	    return;
	}
	
	
	// MUTEX
	
	// Coordinatore
	private void manageResource() throws RemoteException, NotBoundException
	{
		if (this.requests.size() == 0) // nessuna richiesta
			return;
		
		else
		{
			if (this.user == null)
			{
				// la risorsa è disponibile, la assegno al primo che l'aveva richiesta
				int nextUserID = this.requests.get(0);
				Node userStub = (Node) this.registry.lookup(Integer.toString(nextUserID));
				this.user = userStub;
				user.assign();
			}
			else 
			{
				// The coordinator needs to recognize the failure of the node owning the shared resource and, 
				// in case of possible failure, then the coordinator removes the ownership of the resource to the failed node
				if (this.user.getState() == State.FAILED)
				{
					release();
					manageResource();
				}
				else
				{
					// ...
				}
			}
		}
	}
	
	// The other nodes submit repeatedly requests with a random delay between two successive deliveries. 
	private void askResource() throws AccessException, RemoteException, NotBoundException
	{
		try {
			Thread.sleep(MINTIME + this.random.nextInt(MAXTIME - MINTIME));
			} catch (Exception e) { e.printStackTrace(); }
		
		// inserisce il proprio id nella lista delle richieste del coordinatore
		this.coordinator.request(this.ID);
	}
	
	private void useResource()
	{
		System.out.println("Nodo " + this.ID + "usa la risorsa");
		try {
			Thread.sleep(MINTIME + this.random.nextInt(MAXTIME - MINTIME));
			} catch (Exception e) { e.printStackTrace(); }
		
		this.canUse = false;
		
		// TODO: Contatore per rendere possibile la failure durante l'utilizzo di una risorsa
	}
	
	public void request(int id) throws RemoteException
	{
		this.requests.add(id);
	}
	
	public void release()
	{
		this.user = null;
	}
	
	public void assign()
	{
		this.canUse = true;
	}
	
	
	// Esecuzione
	public void run() throws RemoteException, NotBoundException
	{
		// 1)
		this.registry = registryInitialization();
		register();
		
		// 2)

	    
		// 3)
		while (true)
		{
			switch (this.state)
		      {
				case RUNNING:
					
					if (this.startNewElection || this.coordinator == null)
					{
						// non c'è un coordinatore
						election();
						this.startNewElection = false;
					}
					else
					{
						if (this.ID == this.coordinator.getID())
						{
							// questo nodo è il coordinatore, amministrazione della risorsa
							manageResource();
						}
						else
						{
							// questo nodo non è il coordinatore
							if (this.coordinator.getState() == State.FAILED)
							{
								// Failure del coordinatore,serve una nuova elezione
								election();
							}
							else
							{
								// il coordinatore è attivo e richiedo o uso la risorsa se disponibile
								if (this.canUse)
									useResource();
								else
									askResource();
							}
							
						}
					}
					
					break;
					
				case FAILED: 
					
					try { // a node flips repeatedly its “change state” coin with a random delay between two successive flips
					Thread.sleep(MINTIME + this.random.nextInt(MAXTIME - MINTIME));
					} catch (Exception e) { e.printStackTrace(); }
					break;
				
		        default:
		        	break;
		      }
			
			liveOrDie();
		}
		
		
	}
}
