/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.pingoptimizer.pro.shizuku;
public interface IUserService extends android.os.IInterface
{
  /** Default implementation for IUserService. */
  public static class Default implements com.pingoptimizer.pro.shizuku.IUserService
  {
    /**
     * Force-stops a package the same way `adb shell am force-stop <pkg>` would.
     * Unlike ActivityManager.killBackgroundProcesses() (which the OS is free to
     * ignore/undo within seconds), this genuinely tears the process down - it
     * will not silently restart the way "fake booster" apps' cleanup does.
     */
    @Override public void forceStopPackage(java.lang.String packageName) throws android.os.RemoteException
    {
    }
    /** Required by Shizuku so it can cleanly tear down the remote service. */
    @Override public void destroy() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.pingoptimizer.pro.shizuku.IUserService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.pingoptimizer.pro.shizuku.IUserService interface,
     * generating a proxy if needed.
     */
    public static com.pingoptimizer.pro.shizuku.IUserService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.pingoptimizer.pro.shizuku.IUserService))) {
        return ((com.pingoptimizer.pro.shizuku.IUserService)iin);
      }
      return new com.pingoptimizer.pro.shizuku.IUserService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_forceStopPackage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.forceStopPackage(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_destroy:
        {
          this.destroy();
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.pingoptimizer.pro.shizuku.IUserService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
       * Force-stops a package the same way `adb shell am force-stop <pkg>` would.
       * Unlike ActivityManager.killBackgroundProcesses() (which the OS is free to
       * ignore/undo within seconds), this genuinely tears the process down - it
       * will not silently restart the way "fake booster" apps' cleanup does.
       */
      @Override public void forceStopPackage(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_forceStopPackage, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Required by Shizuku so it can cleanly tear down the remote service. */
      @Override public void destroy() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_forceStopPackage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_destroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777114);
  }
  public static final java.lang.String DESCRIPTOR = "com.pingoptimizer.pro.shizuku.IUserService";
  /**
   * Force-stops a package the same way `adb shell am force-stop <pkg>` would.
   * Unlike ActivityManager.killBackgroundProcesses() (which the OS is free to
   * ignore/undo within seconds), this genuinely tears the process down - it
   * will not silently restart the way "fake booster" apps' cleanup does.
   */
  public void forceStopPackage(java.lang.String packageName) throws android.os.RemoteException;
  /** Required by Shizuku so it can cleanly tear down the remote service. */
  public void destroy() throws android.os.RemoteException;
}
