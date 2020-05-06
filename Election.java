package assegnamento2;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Election extends Remote
{
	public int getNodeID() throws RemoteException;
	
	void setCoordinator(final Mutex m) throws RemoteException;
	
	void election(final Election e) throws RemoteException;

	void ok(final Election e) throws RemoteException;
}
