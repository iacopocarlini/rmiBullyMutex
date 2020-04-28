package assegnamento2;

import java.rmi.Remote;
import java.rmi.RemoteException;

import assegnamento2.NodeImpl.State;

public interface Node extends Remote
{
	public int getID() throws RemoteException;
	
	public State getState() throws RemoteException;
	
	// election
	public void startElection() throws RemoteException;
	
	public void setCoordinator(int id) throws RemoteException;
	
	// mutex
	public void request(int id) throws RemoteException;
	
	public void release() throws RemoteException;
	
	public void assign() throws RemoteException;
}
