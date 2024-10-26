function BenefitsCard({ title, text, iconSrc }) {
  return (
    <div className="bg-secondaryBackground shadow-lg flex flex-col gap-2 md:gap-4 items-center rounded-lg p-4 md:p-6 hover:shadow-xl transition-shadow duration-300">
      <h3 className="text-base md:text-xl font-semibold mb-2 text-primaryText">
        {title}
      </h3>
      <p className="text-secondaryText mb-4 text-center text-sm md:text-base">{text}</p>
      <div>
        <img src={iconSrc} alt={title} className="w-8 md:w-12 aspect-square" />
      </div>
    </div>
  );
}

export default BenefitsCard;
