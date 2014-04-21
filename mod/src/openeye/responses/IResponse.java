package openeye.responses;

import openeye.logic.IContext;
import openeye.logic.ITypedStruct;

public interface IResponse extends ITypedStruct {

	public void execute(IContext context);

}
