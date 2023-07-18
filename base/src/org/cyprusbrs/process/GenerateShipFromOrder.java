package org.cyprusbrs.process;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.cyprusbrs.model.MDocType;
import org.cyprusbrs.model.MInOut;
import org.cyprusbrs.model.MInOutLine;
import org.cyprusbrs.model.MOrder;
import org.cyprusbrs.model.MOrderLine;
import org.cyprusbrs.util.Env;

public class GenerateShipFromOrder extends SvrProcess {

	
	
	Integer recordId=0;
	/**	The current Shipment	*/
	private MInOut 		m_shipmentMR = null;
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
		if(m_shipmentMR!=null)
		return "Created Shipment "+m_shipmentMR.getDocumentNo();
		else
		return "Not Created Shipment ";
	}

	/**
	 * Create Shipment/MR header and lines
	 * @param order
	 * @param line
	 */
	private void createLine(MOrder order, MOrderLine orderLine) {
		
		if (m_shipmentMR == null)
		{
			MDocType docType=MDocType.get(getCtx(), order.getC_DocTypeTarget_ID());
			Integer docTypeId=docType.getC_DocTypeShipment_ID();

			log.info(" Doc Type Qty :::: "+docTypeId);
			
			m_shipmentMR = new MInOut (order, docTypeId, m_movementDate);
			m_shipmentMR.setM_Warehouse_ID(orderLine.getM_Warehouse_ID());	//	sets Org too
			if (order.getC_BPartner_ID() != orderLine.getC_BPartner_ID())
				m_shipmentMR.setC_BPartner_ID(orderLine.getC_BPartner_ID());
			if (order.getC_BPartner_Location_ID() != orderLine.getC_BPartner_Location_ID())
				m_shipmentMR.setC_BPartner_Location_ID(orderLine.getC_BPartner_Location_ID());
			if (!m_shipmentMR.save())
				throw new IllegalStateException("Could not create Shipment");
		}
		MInOutLine line = new MInOutLine (m_shipmentMR);
		
		BigDecimal 	toDeliver=orderLine.getQtyEntered().subtract(orderLine.getQtyDelivered());
		log.info("Delivery Qty "+toDeliver);
		line.setOrderLine(orderLine, 0, order.isSOTrx() ? toDeliver : Env.ZERO);
		line.setQty(toDeliver);
		if (!line.save())
		 throw new IllegalStateException("Could not create Shipment Line");
		
	}

}
