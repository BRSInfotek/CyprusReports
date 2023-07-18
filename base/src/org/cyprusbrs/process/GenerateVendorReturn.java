package org.cyprusbrs.process;

import java.sql.Timestamp;

import org.cyprusbrs.model.MDocType;
import org.cyprusbrs.model.MInOut;
import org.cyprusbrs.model.MInOutLine;
import org.cyprusbrs.model.MRMA;
import org.cyprusbrs.model.MRMALine;
import org.cyprusbrs.util.Env;

public class GenerateVendorReturn extends SvrProcess {

private Integer recordId=0;
	
	/** Movement Date			*/
	private Timestamp	m_movementDate = null;
	
	/**	The current Shipment	*/
	private MInOut 	m_CustomerRet = null;
	@Override
	protected void prepare() {
		
		recordId=getRecord_ID();
		m_movementDate = Env.getContextAsDate(getCtx(), "#Date");
		if (m_movementDate == null)
			m_movementDate = new Timestamp(System.currentTimeMillis());

	}

	@Override
	protected String doIt() throws Exception {
		
		MRMA mrma=new MRMA(getCtx(), recordId, get_TrxName());
		
		MRMALine[] lines=mrma.getLines(true);
		
		log.info("No of RMA line "+lines.length);
		if(lines.length>0)
		{
			for(MRMALine line:lines)
			{
				createLine(mrma,line);
			}
		}
		if(m_CustomerRet!=null)
		return "Created Return "+m_CustomerRet.getDocumentNo();
		else
		return "Not Created Return ";
	}
	/**
	 * Create Lines
	 * @param mrma
	 * @param line
	 */
	private void createLine(MRMA mrma, MRMALine line) {

		if (m_CustomerRet == null)
		{
			MDocType docType=MDocType.get(getCtx(), mrma.getC_DocType_ID());
			Integer docTypeId=docType.getC_DocTypeShipment_ID();

			log.info(" Doc Type Qty :::: "+docTypeId);

			m_CustomerRet=new MInOut(mrma, docTypeId, m_movementDate);
			if (!m_CustomerRet.save())
				throw new IllegalStateException("Could not create Return");
		}

		MInOutLine lineRet = new MInOutLine (m_CustomerRet);
		lineRet.setRMALine(line, line.getM_Locator_ID(), line.getQty());
		if (!lineRet.save())
			throw new IllegalStateException("Could not create Shipment Line");		
	}
}
