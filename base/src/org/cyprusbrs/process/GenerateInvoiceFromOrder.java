package org.cyprusbrs.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.cyprusbrs.model.MDocType;
import org.cyprusbrs.model.MInvoice;
import org.cyprusbrs.model.MInvoiceLine;
import org.cyprusbrs.model.MOrder;
import org.cyprusbrs.model.MOrderLine;
import org.cyprusbrs.util.Env;

public class GenerateInvoiceFromOrder extends SvrProcess {

	Integer recordId=0;
	/**	The current Shipment	*/
	private MInvoice 		m_Invoice = null;
	/** Movement Date			*/
	private Timestamp	m_movementDate = null;
	
	/** The date to calculate the days due from			*/
	private Timestamp	p_DateInvoiced = null;
	
	private String vendorInvoiceNumber=null;
	
	@Override
	protected void prepare() {
		
		recordId=getRecord_ID();
		
		m_movementDate = Env.getContextAsDate(getCtx(), "#Date");
		if (m_movementDate == null)
			m_movementDate = new Timestamp(System.currentTimeMillis());
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("DocumentNo"))
				vendorInvoiceNumber = para[i].getParameter().toString();
			else if (name.equals("DateInvoiced"))
				p_DateInvoiced = (Timestamp)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
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
			Integer docType=0;
			if(order.isSOTrx())
				docType=MDocType.getDocType(MDocType.DOCBASETYPE_ARInvoice);
			else
				docType=MDocType.getDocType(MDocType.DOCBASETYPE_APInvoice);	
			log.info(" Doc Type Qty :::: "+docType);			
			m_Invoice = new MInvoice (order, docType, m_movementDate);
			m_Invoice.setM_PriceList_ID(order.getM_PriceList_ID());
			if (order.getC_BPartner_ID() != orderLine.getC_BPartner_ID())
				m_Invoice.setC_BPartner_ID(orderLine.getC_BPartner_ID());
			if (order.getC_BPartner_Location_ID() != orderLine.getC_BPartner_Location_ID())
				m_Invoice.setC_BPartner_Location_ID(orderLine.getC_BPartner_Location_ID());
			// Updated the code as discussed with Surya
			/**
			 * When click on the 'Generate Invoice to Vendor'  button the system will ask the couple of mandatory parameter for generating the AP Invoice as follows:
				1. Vendor Invoice Number
				2. Invoice Date
			 *@author Mukesh
			 *@Date 20230719
			 */
			if(vendorInvoiceNumber!=null && vendorInvoiceNumber.length()>0)
				m_Invoice.setDocumentNo(vendorInvoiceNumber);
			if(p_DateInvoiced!=null)
				m_Invoice.setDateInvoiced(p_DateInvoiced);
			//End of the code
			
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
