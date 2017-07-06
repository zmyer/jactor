/*
 * Created on 20/02/2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package au.com.terabit.jproactor;

// TODO: 17/5/15 by zmyer
public interface AsynchTimerHandler {
    /**
     * Notifies that requested timer has expired.
     *
     * @param opTimer timer operation
     */
    public void timerExpired(OpTimer opTimer) throws Exception;

}
