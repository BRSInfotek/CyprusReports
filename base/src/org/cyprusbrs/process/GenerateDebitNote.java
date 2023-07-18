package org.cyprusbrs.process;

import java.util.logging.Level;

import org.cyprusbrs.model.MDocType;
import org.cyprusbrs.model.MInvoice;
import org.cyprusbrs.model.MInvoiceLine;
import org.cyprusbrs.model.MRMA;
import org.cyprusbrs.model.MRMALine;

public class GenerateDebitNote extends SvrProcess {

	
	/**	Shipment					*/
	private int 	p_M_RMA_ID = 0;
	/**	Price List Version			*/
	private int		p_M_PriceList_ID = 0;
	/* Document No					*/
	private String	p_InvoiceDocumentNo = null;
	
	
	
	@Override
	protected void prepare() {

		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_PriceList_ID"))
				p_M_PriceList_ID = para[i].getParameterAsInt();
			else if (name.equals("InvoiceDocumentNo"))
				p_InvoiceDocumentNo = (String)para[i].getParameter();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		p_M_RMA_ID = getRecord_ID();
	

	}

	@Override
	protected String doIt() throws Exception {
		log.info("M_RMA_ID=" + p_M_RMA_ID 
				+ ", M_PriceList_ID=" + p_M_PriceList_ID
				+ ", InvoiceDocumentNo=" + p_InvoiceDocumentNo);
			if (p_M_RMA_ID == 0)
				throw new IllegalArgumentException("No RMA");
			//
			
			MRMA mrma =new MRMA(getCtx(), p_M_RMA_ID, null); 
			
			MDocType docType=new MDocType(getCtx(), mrma.getC_DocType_ID(), null);
			
			if (mrma.get_ID() == 0)
				throw new IllegalArgumentException("RMA not found");
			if (!MRMA.DOCSTATUS_Completed.equals(mrma.getDocStatus()))
				throw new IllegalArgumentException("RMA not completed");
			MInvoice invoice = new MInvoice(getCtx(),0, get_TrxName());
			invoice.setRMA(mrma); 
		    log.info("Debit Note DocType "+docType.getC_DocTypeInvoice_ID());
			invoice.setC_DocTypeTarget_ID(docType.getC_DocTypeInvoice_ID());
			
		    // Should not override pricelist for RMA
			if (p_M_PriceList_ID != 0)
				invoice.setM_PriceList_ID(p_M_PriceList_ID);
			if (p_InvoiceDocumentNo != null && p_InvoiceDocumentNo.length() > 0)
				invoice.setDocumentNo(p_InvoiceDocumentNo);
			if (!invoice.save())
				throw new IllegalArgumentException("Cannot save Debit Note");
			
			/// Create Credit Note Lines
			
			if(invoice!=null)
			{	

				MRMALine rmaLines[] = mrma.getLines(true);
				for (MRMALine rmaLine : rmaLines)
				{
					
					if (rmaLine.getM_InOutLine_ID() == 0)
		            {
		                throw new IllegalStateException("No customer return line - RMA = " 
		                        + mrma.getDocumentNo() + ", Line = " + rmaLine.getLine());
		            }
					MInvoiceLine invLine = new MInvoiceLine(invoice);
		            invLine.setRMALine(rmaLine);
		            
		            if (!invLine.save())
		            {
		                throw new IllegalStateException("Could not create Debit Note line");
		            }
				}
			}
			
		return "Debit Note "+invoice.getDocumentNo();
	}

}
