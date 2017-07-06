/*
 * Created on 20/02/2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package au.com.terabit.jproactor;

public interface AsynchConnectHandler {
    /**
     * Notifies that connect operation has finished.
     * Called by {@link IOOperation#onComplete}
     *
     * @param opConnect connect operation result
     */
    public void connectCompleted(OpConnect opConnect) throws Exception;

}
