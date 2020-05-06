package assegnamento2;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Mutex extends Remote
{
	  public int getNodeID() throws RemoteException;
	
	  void requestResource(final Mutex m) throws RemoteException;
	  
	  void resourceAvailable(final Mutex m) throws RemoteException;
	
	  void releaseResource(final Mutex m) throws RemoteException;
	}
