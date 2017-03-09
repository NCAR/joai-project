"""
tool written in python to read a tab-delimited spreadsheet having at least
the columns:
	- selected
	- filePath
	
for each selected record, copy the file at filePath to specified stdsDir
"""
import os, sys, shutil, UserDict

class StandardsFileTool:

	"""
	exportedRegistry - tab delimited file containing recs for standards docs in a registry
	stdsDir - destination where selected standards docs are to be written
	"""
	
	def __init__ (self, exportedRegistry, stdsDir):
		self.exportedRegistry = exportedRegistry
		self.stdsDir = stdsDir
		self.schema = []
		# UserDict.__init__ (self)
		self.setup ()
		self.process ()
	
	def setup (self):
		if not os.path.exists (self.exportedRegistry):
			raise Exeption, "exportedRegistry does not exist at" % self.exportedRegistry
		if os.path.exists (self.stdsDir):
			print 'stdsDir exists, deleting ...'
			shutil.rmtree (self.stdsDir)
		os.mkdir (self.stdsDir)
		
	def getField (self, field, row):
		try:
			i = self.schema.index (field)
		except:
			raise Exception, "schema field not found: '%s'" % field
		return row[i]
	
	def process (self):
		s = open (self.exportedRegistry,'r').read()
		lines = s.split ('\n')
		self.schema = lines[0].split ('\t')
		print 'SCHEMA: %s' % self.schema
		rows = lines[1:]
		print "%d records read" % len(rows)
		for row in rows:
			fields = row.split('\t')
			if self.getField ( "selected", fields):
				src = self.getField ('filePath', fields )
				if src[-1] in ['"', "'"] and src[0] == src[-1]:
					src = src[1:-1]
				# if pc paths, convert
				src = src.replace ("\\", "/")
				fileName = os.path.basename (src)
				dst = os.path.join (self.stdsDir, fileName)
				print "writing %s to %s" % (fileName, dst)
				shutil.copyfile(src, dst)
	
if __name__ == '__main__':
	
	# tab delimited file containing recs for all docs in registry
	export = "C:/tmp/ASN/ASN-v2.0"
	dst = "C:/tmp/ASN/MAST-std-docs"
	StandardsFileTool(export, dst)
	
