package com.dropbox.core.v2;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWrappedException;
import com.dropbox.core.stone.StoneSerializer;
import com.dropbox.core.v2.DbxClientV2.DbxUserRawClientV2;

public interface RCPMock {
	<ArgT, ResT, ErrT> ResT rpcStyle(String host, String path, ArgT arg, boolean noAuth,
    		StoneSerializer<ArgT> argSerializer, StoneSerializer<ResT> responseSerializer,
    		StoneSerializer<ErrT> errorSerializer, DbxUserRawClientV2 client) throws DbxWrappedException, DbxException;
}
