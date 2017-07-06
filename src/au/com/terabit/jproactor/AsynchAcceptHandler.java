/*
 * Created on 20/02/2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package au.com.terabit.jproactor;

// TODO: 17/5/15 by zmyer
public interface AsynchAcceptHandler {
    /**
     * Notifies that accept operation has finished.
     * Called by {@link IOOperation#onComplete}
     *
     * @param opAccept accept operation result
     */
    public void acceptCompleted(OpAccept opAccept) throws Exception;

}
