package openeye.responses;

import openeye.logic.IContext;
import openeye.protocol.responses.IResponse;

public interface IExecutableResponse extends IResponse {
	public void execute(IContext context);
}
