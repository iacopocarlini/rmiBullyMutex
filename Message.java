package assegnamento2;

import java.io.Serializable;

public class Message implements Serializable
{
	  private static final long serialVersionUID = 1L;

	  public enum Type
	  {
		COORDINATION, // election
	    ELECTION, 
	    OK, 
	    REQUEST,  // mutex
	    ISAVAILABLE, 
	    RELEASE;
	  }
	
	  private Type    type;
	  private Election el;
	  private Mutex mx;
	

	  public Message(final Type t, final Election e, final Mutex m)
	  {
	    this.type = t;
	    this.el = e;
	    this.mx = m;
	  }
	
	  
	  public Type getType()
	  {
	    return this.type;
	  }
	  
	  public Election getElectionRmObj()
	  {
	    return this.el;
	  }
	  
	  public Mutex getMutexRmObj()
	  {
	    return this.mx;
	  }
}
