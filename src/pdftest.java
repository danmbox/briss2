import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

public class pdftest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/* chapter02/HelloWorldReader.java */
		PdfReader reader;
		try {
			String filename = "BeginningAndroid.pdf";
			reader = new PdfReader(filename);

			PdfStamper stamper = new PdfStamper(reader,
					new FileOutputStream("out.pdf"));

			int n = reader.getNumberOfPages();
			PdfDictionary pageDict;
			PdfArray old_mediabox, new_mediabox, old_cropbox, new_cropbox;
			PdfNumber value;
			BaseFont font = BaseFont.createFont(BaseFont.HELVETICA,
					BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
			PdfContentByte directcontent;
			for (int i = 1; i <= n; i++) {
				pageDict = reader.getPageN(i);
				new_mediabox = new PdfArray();
				old_mediabox = pageDict.getAsArray(PdfName.MEDIABOX);
				value = (PdfNumber) old_mediabox.getAsNumber(0);
				new_mediabox.add(new PdfNumber(value.floatValue() - 300));
				value = (PdfNumber) old_mediabox.getAsNumber(1);
				new_mediabox.add(new PdfNumber(value.floatValue() - 300));
				value = (PdfNumber) old_mediabox.getAsNumber(2);
				new_mediabox.add(new PdfNumber(value.floatValue() + 300));
				value = (PdfNumber) old_mediabox.getAsNumber(3);
				new_mediabox.add(new PdfNumber(value.floatValue() + 300));
				pageDict.put(PdfName.MEDIABOX, new_mediabox);
				
				new_cropbox = new PdfArray();
				old_cropbox = pageDict.getAsArray(PdfName.CROPBOX);
				value = (PdfNumber) old_cropbox.getAsNumber(0);
				new_cropbox.add(new PdfNumber(value.floatValue()+50));
				value = (PdfNumber) old_cropbox.getAsNumber(1);
				new_cropbox.add(new PdfNumber(value.floatValue()+50));
				value = (PdfNumber) old_cropbox.getAsNumber(2);
				new_cropbox.add(new PdfNumber(value.floatValue()-50));
				value = (PdfNumber) old_cropbox.getAsNumber(3);
				new_cropbox.add(new PdfNumber(value.floatValue()-50));
				pageDict.put(PdfName.CROPBOX, new_cropbox);
				
				//
				// If Not
				// pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX) Is
				// Nothing
				// Then
				// newCropBox = New iTextSharp.text.pdf.PdfArray
				// oldCropBox =
				// pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX).ArrayList
				// value = CType(oldCropBox(0), iTextSharp.text.pdf.PdfNumber)
				// newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
				// value = CType(oldCropBox(1), iTextSharp.text.pdf.PdfNumber)
				// newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
				// value = CType(oldCropBox(2), iTextSharp.text.pdf.PdfNumber)
				// newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
				// value = CType(oldCropBox(3), iTextSharp.text.pdf.PdfNumber)
				// newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
				//
				// pageDict.Put(iTextSharp.text.pdf.PdfName.CROPBOX, newCropBox)
				// End If
				//
//				If Not pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX) Is Nothing
//				Then
//				    newCropBox = New iTextSharp.text.pdf.PdfArray
//				    oldCropBox =
//				pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX).ArrayList
//				    value = CType(oldCropBox(0), iTextSharp.text.pdf.PdfNumber)
//				    newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
//				    value = CType(oldCropBox(1), iTextSharp.text.pdf.PdfNumber)
//				    newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
//				    value = CType(oldCropBox(2), iTextSharp.text.pdf.PdfNumber)
//				    newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
//				    value = CType(oldCropBox(3), iTextSharp.text.pdf.PdfNumber)
//				    newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
	//
//				    pageDict.Put(iTextSharp.text.pdf.PdfName.CROPBOX, newCropBox)
//				End If

				directcontent = stamper.getOverContent(i);
				directcontent.beginText();
				directcontent.setFontAndSize(font, 12);
				directcontent.showTextAligned(Element.ALIGN_LEFT, "TEST", 0,
						-18, 0);
				directcontent.endText();
			}
			stamper.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
