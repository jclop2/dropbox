package com.fathzer.soft.jclop.dropbox;

import java.util.Set;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWrappedException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.stone.StoneSerializer;
import com.dropbox.core.v2.RCPMock;
import com.dropbox.core.v2.DbxClientV2.DbxUserRawClientV2;
import com.dropbox.core.v2.common.RootInfo;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.users.Name;
import com.dropbox.core.v2.userscommon.AccountType;

final class CurrentAccountRCPMock implements RCPMock {
	public String displayName;
	public Set<String> acceptedTokens;
	
	@Override
	public <ArgT, ResT, ErrT> ResT rpcStyle(String host, String path, ArgT arg, boolean noAuth,
			StoneSerializer<ArgT> argSerializer, StoneSerializer<ResT> responseSerializer,
			StoneSerializer<ErrT> errorSerializer, DbxUserRawClientV2 client)
			throws DbxWrappedException, DbxException {
		if (path.endsWith("/users/get_current_account")) {
			if (acceptedTokens!=null && !acceptedTokens.contains(getToken(client))) {
				throw new InvalidAccessTokenException("x","x",null);
			}
			return (ResT) new FullAccount("1234567890123456789012345678901234567890", new Name("Jules","Cesar", "fam", displayName, "abr"), "fucking@mail.com", true, false, "fr", "ref", true, AccountType.BASIC, new RootInfo("root", "home"));
		}
		throw new DbxException("Unexpected request "+path);
	}

	private String getToken(DbxUserRawClientV2 client) {
//		return client.credential.getAccessToken();
		return null;
	}
}