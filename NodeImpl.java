package assegnamento2;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class NodeImpl extends UnicastRemoteObject
{

	private static final int PORT = 1099;
	private static int ID;
	private Registry registry;
	private Node coordinator;
	private Random random;
	private boolean startNewElection;
	
	// probabilità
	private static final int H = 10;
	
	public enum State
	  {
		COORDINATOR,
		REQUEST,
	    ELECTION,
	    FAILED;
	  }
	
	private State state;
	
	// Costruttore
	public NodeImpl(int i) throws RemoteException
	{
		this.ID = i;
		this.random = new Random();
		this.startNewElection = false;
	}
	
	public int getID() throws RemoteException
	{
		return this.ID;
	}
	
	
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
	private void timeToDie() throws RemoteException
	{
		int randomNumber = this.random.nextInt(100) + 1;
		
		if (randomNumber < H)
		{
			this.state = State.FAILED;
			System.out.println("Failure nodo " + Integer.toString(this.ID));
		}
		else
			startElection();
			
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
	    	if (this.ID < stub.getID())
	    	{
		    	try {
			    	stub.startElection();
			    	anybodyAnswered = true;
				} catch (RemoteException e) {
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

		this.state = State.COORDINATOR;
		
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
	}
	
	// Esecuzione
	public void run() throws RemoteException
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
				case COORDINATOR:
					break;
					
				case ELECTION:
					break;
					
				case REQUEST:
					break;
					
				case FAILED:
					break;
				
		        default:
		        	break;
		      }
			
			timeToDie();
			
		}
		
		
	}
}
