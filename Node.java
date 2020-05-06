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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import assegnamento2.Message.Type;


public class Node
{

	private static final long serialVersionUID = 1L;
	
	private static final int PORT = 1099;
	
	// sleep in failure
    private static final int MIN = 500;
    private static final int DURATION = 2000;
   
    
	private static int ID;
	private Registry registry;
	
	private Random random;
	
	// messaggi
	private BlockingQueue<Message> messageQueue;
	
	// elezione
	private Election myElection;
	
	// mutex
	private Mutex myMutex;
	private Mutex coordinatorMutex;
	private Mutex resourceUser;
	private List<Mutex> resourceRequests;
	private static final int USAGE_DURATION = 4000;
	
	// probabilità
	private static final int H = 2;
	private static final int K = 90;
	
	// Stati
	public enum State
	  {
		IDLE, // Running ...
		CANDIDATE, // Election
		COORDINATOR, // Election e mutex
		REQUESTER,  // Mutex
		WAITER, // Mutex
	    FAILED; // Failure
	  }
	
	private State state;
	
	// attesa
	private int timeout;
    private static final int CANDIDATE_TIMEOUT = 7;
    private static final int USER_TIMEOUT = 7;
    private static final int MAX_WAIT = 7;
	
	// Costruttore
	public Node(int i, final State s) throws RemoteException
	{
		this.ID = i;
		this.state = s;
		this.timeout = 0;
		
		this.messageQueue = new LinkedBlockingQueue<Message>();
		
		this.random = new Random();
		
		this.myElection = new ElectionImpl(this.ID, this.messageQueue);
	    this.myMutex    = new MutexImpl(this.ID, this.messageQueue);
		
		this.coordinatorMutex = null;
		
		// coordinatore
		this.resourceUser = null;
		this.resourceRequests = new ArrayList<>();
		
		
	}
	

	
	public int getID() throws RemoteException
	{
		return this.ID;
	}
	
	public State getState() throws RemoteException
	{
		return this.state;
	}
	

	
	private Registry registryInitialization() throws RemoteException
	{
		Registry reg = null;
		try {
			reg = LocateRegistry.createRegistry(PORT);
			System.out.println("Nodo " + Integer.toString(this.ID) + ": registro creato");
		} catch (Exception ex){
			try {
				reg = LocateRegistry.getRegistry(PORT);
				System.out.println("Nodo " + Integer.toString(this.ID) + ": registro ottenuto");
			} catch (RemoteException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		return reg;
	}
	
	private void register() throws RemoteException
	{
		try {
			this.registry.rebind("E" + this.ID, this.myElection);
		    this.registry.rebind("M" + this.ID, this.myMutex);
		} 
		catch (RemoteException e1)
		{
			e1.printStackTrace();
		}
	}
	
	
	/*
	 A “running” node can move in a “failed” state with probability H. A “failed” node can move in the “running state” 
	 with probability K. To simulate this behavior, a node flips repeatedly its “change state” coin with a random delay 
	 between two successive flips. Of course, when a node moves to the “running” state, then it starts a new election. 
	 */
	private void liveOrDie() throws RemoteException, NotBoundException
	{
		int randomNumber = this.random.nextInt(100) + 1; // nel range [1, 100]
		// int randomNumber = this.random.nextInt();
		
		switch(this.state)
		{
			case FAILED:
				
				if (randomNumber >= K) // ritorno in attività dalla failure
				{
					System.out.println("++ Nodo " + Integer.toString(this.ID) + " è tornato in funzione, richiedo l'elezione"); //debug
					
					for (Election el : getNodesList(this.ID))
		              el.election(this.myElection);
					
					this.state = State.IDLE;
					
					// setup
					this.timeout = 0;
					this.messageQueue.clear();
				}
				break;
			
			default: // tutti gli altri stati
				
				if (randomNumber <= H)
				{
					System.out.println("-- Failure nodo " + Integer.toString(this.ID));
					this.state = State.FAILED;
				}
				break;
		}
	}

	
	private void idle() 
	{
		try
	    {
	      Message extractedMessage = this.messageQueue.poll();

	      if (extractedMessage == null)
	        return;
	      
	      Type t = extractedMessage.getType();
	      
	      switch(t)
	      {
	    	  case COORDINATION:
	    		  runCoordination(extractedMessage);
	    		  break;
	    		  
	    	  case ELECTION:
	    		  runElection(extractedMessage);
	    		  break;
	    	
	    	  default:
	    		  break;
	      }
	    }
	    catch (Exception e) { e.printStackTrace(); }
	}
	
	
	// ELECTION
	
	private List<Election> getNodesList(final int id) throws RemoteException, NotBoundException
	  {
	    List<Election> nodeList = new ArrayList<>();
	    List<String> electionNames = Arrays.asList(this.registry.list());
	
	    for (String name : electionNames)
	      if (name.charAt(0) == 'E' && (Integer.valueOf(name.substring(1)) != id))
	    	  nodeList.add((Election) this.registry.lookup(name));
	
	    return nodeList;
	  }
	
	private List<Election> bullyCandidates(final int id) throws RemoteException, NotBoundException
	  {
		List<Election> nodeList = new ArrayList<>();
	    List<String> electionNames = Arrays.asList(this.registry.list());
	
	    for (String name : electionNames)
	    	if (name.charAt(0) == 'E' && (Integer.valueOf(name.substring(1)) > id))
	    		nodeList.add((Election) this.registry.lookup(name));
	
	    return nodeList;
	  }
	
	private void runElection(Message m) throws RemoteException, NotBoundException
	{
		this.timeout = 0;

	    if (m.getElectionRmObj().getNodeID() > this.ID)
	      this.state = State.IDLE;
	    
	    else // candidato per il bully
	    {
	      this.state = State.CANDIDATE;
	      ((Election) m.getElectionRmObj()).ok(this.myElection);
	      
	      System.out.println("Nodo " + Integer.toString(this.ID) 
	      					+ ": Sono candidato, mando ok a " 
      						+ Integer.toString(m.getElectionRmObj().getNodeID())); // debug
	    }
	}
	
	private void receiveOK(final Message m) throws RemoteException
	{
	    this.timeout = 0;

	    if (m.getElectionRmObj().getNodeID() > this.ID)
	      this.state = State.IDLE;
	    
	    else
	    	return;
	}
	
	private void runCoordination(Message m) throws RemoteException, NotBoundException
	{
		this.timeout = 0;

		// utilizzatori
	    if (m.getMutexRmObj().getNodeID() > this.ID)
	    {
	      this.state = State.REQUESTER;
	      this.coordinatorMutex = (Mutex) m.getMutexRmObj();
	      
	      // cancellazione dati coordinazione precedente
	      this.resourceRequests.clear();
	      this.resourceUser  = null;
	    }
	    
	    // coordinatore
	    else if (m.getMutexRmObj().getNodeID() == this.ID)
	      for (Election e : getNodesList(this.ID))
	        e.setCoordinator(this.myMutex);
	    
	}

	private void candidate() throws RemoteException, NotBoundException
	{
		Message extractedMessage = this.messageQueue.poll();

	    if (extractedMessage == null)
	    {
	      this.timeout++;
	      
	      if (this.timeout > CANDIDATE_TIMEOUT)
	      {
	        this.state = State.COORDINATOR;
	        
	        this.messageQueue.clear();
	        this.resourceRequests.clear();
	        this.timeout = 0;
	        this.resourceUser = null;
	        
	        System.out.println("Nodo " + Integer.toString(this.ID) + ": sono il coordinatore"); // debug
	        
	        for (Election e : getNodesList(this.ID))
	          e.setCoordinator(this.myMutex);
	      }
	    }
	    
	    else
	    {
	    	Type t = extractedMessage.getType();
	    	
	    	switch(t)
	    	{
		    	case ELECTION:
		    		runElection(extractedMessage);
		    		break;
		    		
		    	case OK:
		    		receiveOK(extractedMessage);
		    		break;
		    	
		    	case COORDINATION:
		    		runCoordination(extractedMessage);
		    		break;
		    		
				default:
					break;
	    	}
	    }
	}
	
	
	
	// MUTEX
	
	private void manageResource()
	{
		try
	    {
	      Message extractedMessage = this.messageQueue.poll();
	      
	      if (extractedMessage != null)
	      {
	    	  Type t = extractedMessage.getType();
	    	  
	    	  if (this.resourceUser != null)
	    	  {
	    		  switch(t)
					{
						case RELEASE:
							
							if (this.resourceRequests.size() > 0) // prelevo il prossimo richiedente
				            {
				              this.timeout = 0;
				              
				              Mutex mtx = this.resourceRequests.remove(0);
				              mtx.resourceAvailable(this.myMutex);
				              
				              System.out.println("Nodo " + Integer.toString(this.ID) 
				              					 + ": il nodo " + Integer.toString(mtx.getNodeID()) 
				              					 + " ottiene la risorsa");
				            }
							
				            else // risorsa libera ma nessuno in attesa di usarla
				            {
				              this.resourceUser = null;
				              System.out.println("Nodo " + Integer.toString(this.ID) 
				              					 + ": la risorsa è libera, nessuno è in attesa "); // debug
				            }
							
							break;
							
						case REQUEST:
							
							this.timeout++;
							
							if (this.timeout > USER_TIMEOUT)
				            {
				              this.timeout = 0;
				              this.resourceUser = extractedMessage.getMutexRmObj();
				              
				              System.out.println("Nodo " + Integer.toString(this.ID) 
					           					 + ": failure dell'utilizzatore, riassegno la risorsa "); // debug
				            }
							
				            else
				              this.resourceRequests.add(extractedMessage.getMutexRmObj());
							
							break;

						default:
							break;
					}
	    	  }
	    	  
		      else
		      {
			    	
				switch(t)
				{
					case ELECTION:
						runElection(extractedMessage);
						break;
						
					case COORDINATION:
						runCoordination(extractedMessage);
						break;
					
					case REQUEST: // assegnamento risorsa
						System.out.println("Nodo " + Integer.toString(this.ID) + ": (coord) assegno la risorsa al nodo " 
											+ Integer.toString(extractedMessage.getMutexRmObj().getNodeID())); // debug
						this.resourceUser = extractedMessage.getMutexRmObj();
				        this.resourceUser.resourceAvailable(this.myMutex);
						break;
						
					default:
						break;
				}
		      }	      
	      }

	    }
	    catch (Exception e) { e.printStackTrace(); }
	}
	
	private void request()
	{
		try
	    {
	      this.coordinatorMutex.requestResource(this.myMutex);
	      this.state = State.WAITER;
	      System.out.println("Nodo " + Integer.toString(this.ID) 
			 				  + ": richiedo la risorsa, mi metto in attesa "); // debug
	    }
	    catch (Exception e) { e.printStackTrace(); }
	}
	
	private void waitResource()
	{
		try
	    {
	      Message extractedMessage = this.messageQueue.poll();

	      if (extractedMessage == null)
	      {
	    	this.timeout++;
	        if (this.timeout > MAX_WAIT)
	        {
	        	
	          this.state = State.CANDIDATE;

	          for (Election e : bullyCandidates(this.ID))
	            e.election(this.myElection);
	          
	          System.out.println("Nodo " + Integer.toString(this.ID) + ": failure del coordinatore, parte l'elezione"); // debug
	        }
	      }
	      
	      else
	      {
	    	Type t = extractedMessage.getType();
		    	
			switch(t)
			{
				case ELECTION:
					runElection(extractedMessage);
					break;
					
				case COORDINATION:
					runCoordination(extractedMessage);
					break;
				
				case ISAVAILABLE:
					this.timeout = 0;
					
					// ***********
					useResource(); // Utilizzo effettivo della risorsa
					// ***********
					
					this.coordinatorMutex.releaseResource(this.myMutex);
					this.state = State.REQUESTER;
					break;
					
				default:
					break;
			}
	      }
	    }
	    catch (Exception e)
	    {
	      e.printStackTrace();
	      return;
	    }
	}
	
	
	private void useResource()
	{
		// ...
		System.out.println("Nodo " + Integer.toString(this.ID) + ": Ho ottenuto la risorsa, ora faccio qualcosa"); // debug
		
		// doSomething()
		try
		{ 
			// Simulazione utilizzo
			Thread.sleep(MIN + this.random.nextInt(USAGE_DURATION));
		} catch (Exception e) { e.printStackTrace(); }
		
	}

	
	// Esecuzione
	public void run() throws RemoteException, NotBoundException
	{
		// 1) Inizializzazione
		this.registry = registryInitialization();
		register();

		// 2) sincronizzazione iniziale +  successiva esecuzione asincrona
		while (true)
		{
			switch (this.state)
		      {
				  // RUNNING
				  case IDLE:
				      idle();
				      break;
				      
				  case CANDIDATE:
				      candidate();
				      break;
				      
				  case COORDINATOR:
				      manageResource();
				      break;
				      
				  case REQUESTER:
				      request();
				      break;
				      
				  case WAITER:
					  waitResource();
				      break;
				      
				// FAILURE
				case FAILED: 
					try
					{ 
						// a node flips repeatedly its “change state” coin with a random delay between two successive flips
						Thread.sleep(MIN + this.random.nextInt(DURATION));
					} catch (Exception e) { e.printStackTrace(); }
					break;
				
		        default:
		        	break;
		      }
			
			liveOrDie();
			
			// fine while
		}
	
	// fine run
	}

	
	public static void main(final String[] args) throws Exception
	{
		if (Integer.parseInt(args[1]) == 1)
			new Node(Integer.parseInt(args[0]), State.IDLE).run();
		
		else
			new Node(Integer.parseInt(args[0]), State.CANDIDATE).run();
	}
}
