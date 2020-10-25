import sys,os,re

if (len(sys.argv) != 2) :
   print("pass i2p logfile as parameter")
   sys.exit(1)

HISTORY=100

class Prediction :
   def __init__(self,router):
      self.router = router
      self.predicting = False
      self.prediction = None
      self.results = []
      self.S = 0
      self.R = 0
      self.F = 0

   def predict(self, prediction) :
      if self.prediction is None :
         self.predicting = True
         self.prediction = prediction

   def observe(self, observation) :
      if self.prediction is None :
         print("observation without prediction, ignoring")
         return
      success = self.prediction == observation
      self.results.append(success)
      self.prediction = None
      if success :
         if observation == "FAILED" :
            self.F += 1
         elif observation == "REJECT" :
            self.R += 1
         else :
            self.S += 1

   def __str__(self) :
      if len(self.results) == 0 :
         return "no data for %s" % self.router
      successful = 0
      for result in self.results :
          if result :
              successful += 1
      successful = successful * 100.0 / len(self.results)
      return "%s : %.2f%% out of %d  S:%d R:%d F:%d" % \
        (self.router, successful, len(self.results), self.S, self.R, self.F) 

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

 

totalPredictions, totalGood = 0, 0
for _,v in predictions.items() :
   if len(v.results) < HISTORY :
       continue
   print(v)
   totalPredictions += len(v.results)
   for prediction in v.results :
      if prediction :
         totalGood += 1

print("total %d/%d (%.2f%%)" % (totalGood, totalPredictions, totalGood * 100.0 / totalPredictions))
