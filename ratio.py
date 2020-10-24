import sys,os,re

if (len(sys.argv) != 2) :
   print("pass i2p logfile as parameter")
   sys.exit(1)

class Prediction :
   def __init__(self,router):
      self.router = router
      self.predicting = False
      self.prediction = None
      self.results = []

   def predict(self, prediction) :
      if self.prediction is None :
         self.predicting = True
         self.prediction = prediction

   def observe(self, observation) :
      if self.prediction is None :
         print("observation without prediction, ignoring")
         return
      self.results.append( self.prediction == observation)
      self.prediction = None

   def __str__(self) :
      if len(self.results) == 0 :
         return "no data for %s" % self.router
      successful = 0
      for result in self.results :
          if result :
              successful += 1
      successful = successful * 1.0 / len(self.results)
      return "%s : %s out of %d" % (self.router, successful, len(self.results)) 

predictRE = re.compile(".*?Hash: (.*?) predicting (.*)$")
recordRE = re.compile(".*?Hash: (.*?) recording (.*)$")

predictions = {}      

for line in open(sys.argv[1]) :
   match = predictRE.match(line)
   if (match is not None) :
      routerHash = match.groups()[0]
      router = None
      if not predictions.has_key(routerHash) :
         router = Prediction(routerHash)
         predictions[routerHash] = router
      else :
         router = predictions[routerHash]
      router.predict(match.groups()[1])
      continue 
   match = recordRE.match(line)
   if match is not None :
      routerHash = match.groups()[0]
      if not predictions.has_key(routerHash) :
         println("recording without a prediction, ignoring")
         continue
      router = predictions[routerHash]
      router.observe(match.groups()[1])
      continue

 
for _,v in predictions.items() :
   print(v)
