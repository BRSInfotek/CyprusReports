package org.cyprusbrs.process;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.cyprusbrs.model.MDocType;
import org.cyprusbrs.model.MInvoice;
import org.cyprusbrs.model.MInvoiceLine;
import org.cyprusbrs.model.MOrder;
import org.cyprusbrs.model.MOrderLine;
import org.cyprusbrs.util.Env;

public class GenerateInvoiceFromSO extends SvrProcess {

	Integer recordId=0;
	/**	The current Shipment	*/
	private MInvoice 		m_Invoice = null;
	/** Movement Date			*/
	private Timestamp	m_movementDate = null;
	
	@Override
	protected void prepare() {
		
		recordId=getRecord_ID();
		
		m_movementDate = Env.getContextAsDate(getCtx(), "#Date");
		if (m_movementDate == null)
			m_movementDate = new Timestamp(System.currentTimeMillis());
		
	}

	@Override
	protected String doIt() throws Exception {
		
		MOrder order=new MOrder(getCtx(), recordId, get_TrxName());
		MOrderLine[] oLines=order.getLines(true, MOrderLine.COLUMNNAME_Line);
		log.info("No of order line "+oLines.length);
		if(oLines.length>0)
		{
			for(MOrderLine line:oLines)
			{
				createLine(order,line);
			}
		}
		if(m_Invoice!=null)
		return "Created Invoice "+m_Invoice.getDocumentNo();
		else
		return "Not Created Invoice ";
	}

	/**
	 * Created Header and lines of invoice
	 * @param order
	 * @param line
	 */
	private void createLine(MOrder order, MOrderLine orderLine) {

		if (m_Invoice == null)
		{
			MDocType docType=MDocType.get(getCtx(), order.getC_DocTypeTarget_ID());
			Integer docTypeId=docType.getC_DocTypeInvoice_ID();

			log.info(" Doc Type Qty :::: "+docTypeId);
			
			m_Invoice = new MInvoice (order, docTypeId, m_movementDate);
			m_Invoice.setM_PriceList_ID(order.getM_PriceList_ID());
			//			m_Invoice.setM_Warehouse_ID(orderLine.getM_Warehouse_ID());	//	sets Org too
			if (order.getC_BPartner_ID() != orderLine.getC_BPartner_ID())
				m_Invoice.setC_BPartner_ID(orderLine.getC_BPartner_ID());
			if (order.getC_BPartner_Location_ID() != orderLine.getC_BPartner_Location_ID())
				m_Invoice.setC_BPartner_Location_ID(orderLine.getC_BPartner_Location_ID());
			if (!m_Invoice.save())
				throw new IllegalStateException("Could not create Shipment");
		}
		MInvoiceLine line = new MInvoiceLine (m_Invoice);

		BigDecimal 	toDeliver=orderLine.getQtyEntered().subtract(orderLine.getQtyInvoiced());
		log.info("Delivery Qty "+toDeliver);
		line.setOrderLine(orderLine);
		line.setQty(toDeliver);
		if (!line.save())
			throw new IllegalStateException("Could not create Shipment Line");

	}

}
