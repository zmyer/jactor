/*
 * Created on 20/02/2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package au.com.terabit.jproactor;

public interface AsynchReadHandler {
    /**
     * Notifies that read operation has finished.
     * Called by {@link IOOperation#onComplete}
     *
     * @param opRead completed read operation
     */
    public void readCompleted(OpRead opRead) throws Exception;
}
