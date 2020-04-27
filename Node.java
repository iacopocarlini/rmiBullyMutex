package assegnamento2;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Node extends Remote
{
	public void startElection() throws RemoteException;
	
	public int getID() throws RemoteException;
	
	public void setCoordinator(int id) throws RemoteException;
	
}
