package assegnamento2;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;

import assegnamento2.Message.Type;

public class MutexImpl extends UnicastRemoteObject implements Mutex
{
	private static final long serialVersionUID = 1L;

	  private int ID;
	  private BlockingQueue<Message> queue;

	  public MutexImpl(final int i, final BlockingQueue<Message> q) throws RemoteException
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
	  public void requestResource(final Mutex m) throws RemoteException
	  {
	    try
	    {
	      this.queue.put(new Message(Type.REQUEST, null, m));
	    }
	    catch (InterruptedException e)
	    {
	      e.printStackTrace();
	    }
	  }

	  
	  @Override
	  public void resourceAvailable(final Mutex m)
	      throws RemoteException
	  {
	    try
	    {
	      this.queue.put(new Message(Type.ISAVAILABLE, null, m));
	    }
	    catch (InterruptedException e)
	    {
	      e.printStackTrace();
	    }
	  }

	  
	  @Override
	  public void releaseResource(final Mutex m)
	      throws RemoteException
	  {
	    try
	    {
	      this.queue.put(new Message(Type.RELEASE, null, m));
	    }
	    catch (InterruptedException e)
	    {
	      e.printStackTrace();
	    }
	  }
}
