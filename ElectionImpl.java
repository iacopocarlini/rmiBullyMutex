package assegnamento2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;

import assegnamento2.Message.Type;

public class ElectionImpl extends UnicastRemoteObject implements Election
{
	private static final long serialVersionUID = 1L;

	  private BlockingQueue<Message> queue;
	  private int ID;

	  
	  public ElectionImpl(final int i, final BlockingQueue<Message> q)
	      throws RemoteException
	  {
	    this.ID    = i;
	    this.queue = q;
	  }

	  @Override
	  public int getNodeID() throws RemoteException
	  {
	    return this.ID;
	  }

	  @Override
	  public void election(final Election e) throws RemoteException
	  {
	    try
	    {
	      this.queue.put(new Message(Type.ELECTION, e, null));
	    }
	    catch (InterruptedException ex)
	    {
	      ex.printStackTrace();
	    }
	  }

	  @Override
	  public void ok(final Election e) throws RemoteException
	  {
	    try
	    {
	      this.queue.put(new Message(Type.OK, e, null));
	    }
	    catch (InterruptedException ex)
	    {
	      ex.printStackTrace();
	    }
	  }

	  @Override
	  public void setCoordinator(final Mutex m) throws RemoteException
	  {
	    try
	    {
	      this.queue.put(new Message(Type.COORDINATION, null, m));
	    }
	    catch (InterruptedException e)
	    {
	      e.printStackTrace();
	    }
	  }
}
