package openeye.responses;

import openeye.logic.IContext;
import openeye.struct.ITypedStruct;

public interface IResponse extends ITypedStruct {

	public void execute(IContext context);

}
